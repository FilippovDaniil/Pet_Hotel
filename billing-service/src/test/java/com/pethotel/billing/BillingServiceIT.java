package com.pethotel.billing;

import com.pethotel.billing.dto.InvoiceDto;
import com.pethotel.billing.entity.InvoiceStatus;
import com.pethotel.billing.repository.InvoiceRepository;
import com.pethotel.billing.service.BillingService;
import com.pethotel.common.event.BookingCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

// Интеграционный тест BillingService: реальная PostgreSQL + встроенный Kafka.
// Проверяет полный жизненный цикл счёта: создание → накопление → оплата.
//
// @EmbeddedKafka: нужен, потому что BillingService.pay() публикует payment.processed в KafkaTemplate.
// Без @EmbeddedKafka Spring Boot попытается подключиться к реальному Kafka → тест не запустится.
// Достаточно объявить один топик "payment.processed" — остальные (booking.*) billingService не пишет.
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"payment.processed"})
class BillingServiceIT {

    // @ServiceConnection: Spring Boot 3.1+ автоматически конфигурирует datasource из адреса контейнера.
    // withInitScript: создаёт схему "billing" ДО того, как Hibernate делает DDL (CREATE TABLE).
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-billing-schema.sql");

    @Autowired BillingService billingService;
    @Autowired InvoiceRepository invoiceRepository;

    // ── createInvoice ──────────────────────────────────────────────────────────

    // Создаём счёт через service и проверяем реальную запись в PostgreSQL.
    // var (Java 10+) — вывод типа: Invoice без явного объявления типа.
    @Test
    void createInvoice_persistsCorrectAmounts() {
        billingService.createInvoice(completedEvent(100L, 1L,
                new BigDecimal("9000"), new BigDecimal("1000")));

        var invoice = invoiceRepository.findByBookingId(100L).orElseThrow();
        assertThat(invoice.getRoomAmount()).isEqualByComparingTo("9000");
        assertThat(invoice.getAmenitiesAmount()).isEqualByComparingTo("1000");
        assertThat(invoice.getDiningAmount()).isEqualByComparingTo("0");     // dining не начислен
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("10000");  // 9000 + 1000
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    // Идемпотентность: booking.completed может прийти дважды (Kafka at-least-once).
    // Второй вызов обновляет существующий счёт, а не создаёт дубликат.
    @Test
    void createInvoice_calledTwiceForSameBooking_isIdempotent() {
        BookingCompletedEvent event = completedEvent(101L, 1L,
                new BigDecimal("5000"), BigDecimal.ZERO);

        billingService.createInvoice(event);
        billingService.createInvoice(event); // второй вызов — должен игнорироваться (or update)

        // Фильтруем только бронирование 101L: могут быть другие счета из соседних тестов.
        assertThat(invoiceRepository.findByCustomerId(1L)
                .stream().filter(i -> i.getBookingId().equals(101L))).hasSize(1);
    }

    // ── addDiningCharge ────────────────────────────────────────────────────────

    // Два добавления dining-доплаты: 200 + 350 = 550. totalAmount пересчитывается каждый раз.
    @Test
    void addDiningCharge_accumulatesCorrectly() {
        billingService.createInvoice(completedEvent(102L, 1L,
                new BigDecimal("8000"), new BigDecimal("500")));

        billingService.addDiningCharge(102L, new BigDecimal("200")); // первая доплата
        billingService.addDiningCharge(102L, new BigDecimal("350")); // вторая доплата

        var invoice = invoiceRepository.findByBookingId(102L).orElseThrow();
        assertThat(invoice.getDiningAmount()).isEqualByComparingTo("550");    // 200 + 350
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("9050");    // 8000 + 500 + 550
    }

    // Несуществующий bookingId → NoSuchElementException (нет счёта, к которому добавлять).
    @Test
    void addDiningCharge_invoiceNotFound_throwsNoSuchElement() {
        assertThatThrownBy(() -> billingService.addDiningCharge(9999L, new BigDecimal("100")))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ── pay ────────────────────────────────────────────────────────────────────

    // Оплата: статус меняется на PAID и в DTO, и в реальной записи PostgreSQL.
    @Test
    void pay_changesStatusToPaid_andReturnsDto() {
        billingService.createInvoice(completedEvent(103L, 1L,
                new BigDecimal("6000"), new BigDecimal("2000")));

        InvoiceDto result = billingService.pay(103L);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID); // DTO
        assertThat(invoiceRepository.findByBookingId(103L).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.PAID); // реальная запись в БД
    }

    // Повторная оплата → IllegalStateException.
    @Test
    void pay_alreadyPaid_throwsIllegalState() {
        billingService.createInvoice(completedEvent(104L, 1L,
                new BigDecimal("3000"), BigDecimal.ZERO));
        billingService.pay(104L); // первая оплата

        assertThatThrownBy(() -> billingService.pay(104L)) // вторая — ошибка
                .isInstanceOf(IllegalStateException.class);
    }

    // ── full lifecycle ─────────────────────────────────────────────────────────

    // Полный жизненный цикл счёта за одно бронирование:
    //   1. createInvoice: room=10000, amenities=3000
    //   2. addDiningCharge: +500
    //   3. addDiningCharge: +700
    //   4. pay: итого = 10000 + 3000 + 500 + 700 = 14200, статус PAID.
    @Test
    void fullLifecycle_createAddDiningThenPay() {
        billingService.createInvoice(completedEvent(105L, 2L,
                new BigDecimal("10000"), new BigDecimal("3000")));

        billingService.addDiningCharge(105L, new BigDecimal("500"));
        billingService.addDiningCharge(105L, new BigDecimal("700"));

        InvoiceDto paid = billingService.pay(105L);

        assertThat(paid.getTotalAmount()).isEqualByComparingTo("14200"); // 10000+3000+500+700
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    // getByCustomerId: два разных бронирования одного клиента → оба счёта возвращаются.
    @Test
    void getByCustomerId_returnsAllInvoicesForCustomer() {
        billingService.createInvoice(completedEvent(200L, 99L,
                new BigDecimal("4000"), BigDecimal.ZERO));
        billingService.createInvoice(completedEvent(201L, 99L,
                new BigDecimal("5000"), BigDecimal.ZERO));

        assertThat(billingService.getByCustomerId(99L)).hasSize(2);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private BookingCompletedEvent completedEvent(Long bookingId, Long customerId,
                                                  BigDecimal roomTotal, BigDecimal amenitiesTotal) {
        return BookingCompletedEvent.builder()
                .bookingId(bookingId).customerId(customerId)
                .roomTotal(roomTotal).amenitiesTotal(amenitiesTotal)
                .build();
    }
}

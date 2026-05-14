package com.pethotel.billing.service;

import com.pethotel.billing.dto.InvoiceDto;
import com.pethotel.billing.entity.Invoice;
import com.pethotel.billing.entity.InvoiceStatus;
import com.pethotel.billing.repository.InvoiceRepository;
import com.pethotel.common.event.BookingCompletedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit-тест BillingService: изолированный тест логики счётов (без БД, без Kafka).
// Kafka mock нужен для verify в тесте pay() — метод публикует payment.processed.
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks BillingService billingService;

    // ── createInvoice ────────────────────────────────────────────────────────────

    // Новое бронирование → создаём счёт: roomAmount=8000, amenitiesAmount=2000, total=10000.
    // ArgumentCaptor перехватывает объект, переданный в save(), для проверки всех полей.
    @Test
    void createInvoice_newBooking_savesWithCorrectAmounts() {
        when(invoiceRepository.findByBookingId(1L)).thenReturn(Optional.empty()); // счёта ещё нет
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L); // имитируем Hibernate-генерацию id
            return i;
        });

        billingService.createInvoice(completedEvent(1L, 1L, new BigDecimal("8000"), new BigDecimal("2000")));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();
        assertThat(saved.getRoomAmount()).isEqualByComparingTo("8000");
        assertThat(saved.getAmenitiesAmount()).isEqualByComparingTo("2000");
        assertThat(saved.getDiningAmount()).isEqualByComparingTo("0"); // dining ещё не начислен
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("10000"); // 8000 + 2000 + 0
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.UNPAID); // новый счёт не оплачен
    }

    // booking.created создал черновик счёта, booking.completed финализирует суммы.
    // createInvoice обновляет существующий счёт, а не создаёт дубликат.
    @Test
    void createInvoice_alreadyExists_updatesAmountsAndSaves() {
        when(invoiceRepository.findByBookingId(1L))
                .thenReturn(Optional.of(invoice(1L, 1L, InvoiceStatus.UNPAID))); // черновик уже есть
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        billingService.createInvoice(completedEvent(1L, 1L, new BigDecimal("5000"), new BigDecimal("1500")));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getRoomAmount()).isEqualByComparingTo("5000");     // обновилось
        assertThat(captor.getValue().getAmenitiesAmount()).isEqualByComparingTo("1500"); // обновилось
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("6500");     // пересчитано
    }

    // BookingCompletedEvent с null-суммами (edge case) → трактуем как ZERO, не NPE.
    @Test
    void createInvoice_nullAmounts_treatedAsZero() {
        BookingCompletedEvent event = BookingCompletedEvent.builder()
                .bookingId(1L).customerId(1L)
                .roomTotal(null).amenitiesTotal(null) // null — возможно при некорректном событии
                .build();
        when(invoiceRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        billingService.createInvoice(event);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("0"); // null → 0, без NPE
    }

    // ── addDiningCharge ──────────────────────────────────────────────────────────

    // Накопление dining-начислений: 500 (уже есть) + 300 (новое) = 800.
    // totalAmount = roomAmount + amenitiesAmount + diningAmount пересчитывается.
    @Test
    void addDiningCharge_accumulates() {
        Invoice inv = invoice(1L, 1L, InvoiceStatus.UNPAID);
        inv.setRoomAmount(new BigDecimal("8000"));
        inv.setAmenitiesAmount(new BigDecimal("2000"));
        inv.setDiningAmount(new BigDecimal("500"));      // уже есть 500 руб. доплаты
        inv.setTotalAmount(new BigDecimal("10500"));
        when(invoiceRepository.findByBookingId(1L)).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        billingService.addDiningCharge(1L, new BigDecimal("300")); // +300

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getDiningAmount()).isEqualByComparingTo("800");   // 500 + 300
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("10800");  // пересчитано
    }

    // Счёт для данного бронирования не найден → NoSuchElementException с bookingId в сообщении.
    @Test
    void addDiningCharge_invoiceNotFound_throwsNoSuchElement() {
        when(invoiceRepository.findByBookingId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.addDiningCharge(99L, new BigDecimal("100")))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── pay ──────────────────────────────────────────────────────────────────────

    // UNPAID → PAID: статус обновлён + Kafka-событие payment.processed (anyString() — ключ = bookingId).
    @Test
    void pay_unpaidInvoice_setsStatusToPaidAndPublishesEvent() {
        when(invoiceRepository.findByBookingId(1L))
                .thenReturn(Optional.of(invoice(1L, 1L, InvoiceStatus.UNPAID)));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InvoiceDto result = billingService.pay(1L);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        // Проверяем топик + наличие любого события: конкретное содержимое протестируют IT-тесты.
        verify(kafkaTemplate).send(eq(KafkaTopics.PAYMENT_PROCESSED), anyString(), any());
    }

    // Повторная оплата → IllegalStateException "already paid".
    @Test
    void pay_alreadyPaid_throwsIllegalState() {
        when(invoiceRepository.findByBookingId(1L))
                .thenReturn(Optional.of(invoice(1L, 1L, InvoiceStatus.PAID)));

        assertThatThrownBy(() -> billingService.pay(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void pay_invoiceNotFound_throwsNoSuchElement() {
        when(invoiceRepository.findByBookingId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.pay(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getters ──────────────────────────────────────────────────────────────────

    @Test
    void getByBookingId_notFound_throwsNoSuchElement() {
        when(invoiceRepository.findByBookingId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.getByBookingId(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // getByCustomerId: список счётов клиента включает как UNPAID, так и PAID.
    @Test
    void getByCustomerId_returnsMappedList() {
        when(invoiceRepository.findByCustomerId(1L)).thenReturn(List.of(
                invoice(1L, 1L, InvoiceStatus.UNPAID),
                invoice(2L, 1L, InvoiceStatus.PAID)));

        List<InvoiceDto> result = billingService.getByCustomerId(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(result.get(1).getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    // Фабрика BookingCompletedEvent — минимальный набор полей для createInvoice().
    private BookingCompletedEvent completedEvent(Long bookingId, Long customerId,
                                                  BigDecimal roomTotal, BigDecimal amenitiesTotal) {
        return BookingCompletedEvent.builder()
                .bookingId(bookingId).customerId(customerId)
                .roomTotal(roomTotal).amenitiesTotal(amenitiesTotal)
                .build();
    }

    // Заглушка Invoice с предзаполненными суммами — используется в тестах update/pay/getters.
    private Invoice invoice(Long id, Long customerId, InvoiceStatus status) {
        return Invoice.builder()
                .id(id).bookingId(1L).customerId(customerId)
                .roomAmount(new BigDecimal("8000"))
                .amenitiesAmount(new BigDecimal("2000"))
                .diningAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("10000"))
                .status(status)
                .build();
    }
}

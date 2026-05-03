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

/**
 * Integration tests for BillingService with real PostgreSQL and embedded Kafka.
 * Tests the full invoice lifecycle: creation → dining charge accumulation → payment.
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"payment.processed"})
class BillingServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-billing-schema.sql");

    @Autowired BillingService billingService;
    @Autowired InvoiceRepository invoiceRepository;

    // ── createInvoice ──────────────────────────────────────────────────────────

    @Test
    void createInvoice_persistsCorrectAmounts() {
        billingService.createInvoice(completedEvent(100L, 1L,
                new BigDecimal("9000"), new BigDecimal("1000")));

        var invoice = invoiceRepository.findByBookingId(100L).orElseThrow();
        assertThat(invoice.getRoomAmount()).isEqualByComparingTo("9000");
        assertThat(invoice.getAmenitiesAmount()).isEqualByComparingTo("1000");
        assertThat(invoice.getDiningAmount()).isEqualByComparingTo("0");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("10000");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    @Test
    void createInvoice_calledTwiceForSameBooking_isIdempotent() {
        BookingCompletedEvent event = completedEvent(101L, 1L,
                new BigDecimal("5000"), BigDecimal.ZERO);

        billingService.createInvoice(event);
        billingService.createInvoice(event); // second call must be ignored

        assertThat(invoiceRepository.findByCustomerId(1L)
                .stream().filter(i -> i.getBookingId().equals(101L))).hasSize(1);
    }

    // ── addDiningCharge ────────────────────────────────────────────────────────

    @Test
    void addDiningCharge_accumulatesCorrectly() {
        billingService.createInvoice(completedEvent(102L, 1L,
                new BigDecimal("8000"), new BigDecimal("500")));

        billingService.addDiningCharge(102L, new BigDecimal("200"));
        billingService.addDiningCharge(102L, new BigDecimal("350"));

        var invoice = invoiceRepository.findByBookingId(102L).orElseThrow();
        assertThat(invoice.getDiningAmount()).isEqualByComparingTo("550");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("9050");
    }

    @Test
    void addDiningCharge_invoiceNotFound_throwsNoSuchElement() {
        assertThatThrownBy(() -> billingService.addDiningCharge(9999L, new BigDecimal("100")))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ── pay ────────────────────────────────────────────────────────────────────

    @Test
    void pay_changesStatusToPaid_andReturnsDto() {
        billingService.createInvoice(completedEvent(103L, 1L,
                new BigDecimal("6000"), new BigDecimal("2000")));

        InvoiceDto result = billingService.pay(103L);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoiceRepository.findByBookingId(103L).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void pay_alreadyPaid_throwsIllegalState() {
        billingService.createInvoice(completedEvent(104L, 1L,
                new BigDecimal("3000"), BigDecimal.ZERO));
        billingService.pay(104L);

        assertThatThrownBy(() -> billingService.pay(104L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── full lifecycle ─────────────────────────────────────────────────────────

    @Test
    void fullLifecycle_createAddDiningThenPay() {
        billingService.createInvoice(completedEvent(105L, 2L,
                new BigDecimal("10000"), new BigDecimal("3000")));

        billingService.addDiningCharge(105L, new BigDecimal("500"));
        billingService.addDiningCharge(105L, new BigDecimal("700"));

        InvoiceDto paid = billingService.pay(105L);

        assertThat(paid.getTotalAmount()).isEqualByComparingTo("14200");
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

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

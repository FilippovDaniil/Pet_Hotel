package com.pethotel.billing.service;

import com.pethotel.billing.dto.InvoiceDto;
import com.pethotel.billing.entity.Invoice;
import com.pethotel.billing.entity.InvoiceStatus;
import com.pethotel.billing.repository.InvoiceRepository;
import com.pethotel.common.event.BookingCompletedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void createInvoice(BookingCompletedEvent event) {
        log.info("Creating invoice for bookingId={} customerId={}", event.getBookingId(), event.getCustomerId());

        if (invoiceRepository.findByBookingId(event.getBookingId()).isPresent()) {
            log.warn("Invoice already exists for bookingId={}, skipping creation", event.getBookingId());
            return;
        }

        BigDecimal roomAmount       = event.getRoomTotal()       != null ? event.getRoomTotal()       : BigDecimal.ZERO;
        BigDecimal amenitiesAmount  = event.getAmenitiesTotal()  != null ? event.getAmenitiesTotal()  : BigDecimal.ZERO;
        BigDecimal diningAmount     = BigDecimal.ZERO;
        BigDecimal totalAmount      = roomAmount.add(amenitiesAmount).add(diningAmount);

        Invoice invoice = Invoice.builder()
                .bookingId(event.getBookingId())
                .customerId(event.getCustomerId())
                .roomAmount(roomAmount)
                .amenitiesAmount(amenitiesAmount)
                .diningAmount(diningAmount)
                .totalAmount(totalAmount)
                .status(InvoiceStatus.UNPAID)
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created: id={} bookingId={} totalAmount={}", invoice.getId(), invoice.getBookingId(), totalAmount);
    }

    @Transactional
    public void addDiningCharge(Long bookingId, BigDecimal amount) {
        log.info("Adding dining charge bookingId={} amount={}", bookingId, amount);
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for bookingId: " + bookingId));

        invoice.setDiningAmount(invoice.getDiningAmount().add(amount));
        invoice.setTotalAmount(invoice.getRoomAmount()
                .add(invoice.getAmenitiesAmount())
                .add(invoice.getDiningAmount()));
        invoiceRepository.save(invoice);
        log.info("Dining charge added: invoiceId={} newDiningAmount={} newTotal={}",
                invoice.getId(), invoice.getDiningAmount(), invoice.getTotalAmount());
    }

    @Transactional
    public InvoiceDto pay(Long bookingId) {
        log.info("Processing payment for bookingId={}", bookingId);
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for bookingId: " + bookingId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice already paid for bookingId: " + bookingId);
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice = invoiceRepository.save(invoice);

        kafkaTemplate.send(KafkaTopics.PAYMENT_PROCESSED, String.valueOf(bookingId),
                Map.of("bookingId", bookingId, "invoiceId", invoice.getId(), "totalAmount", invoice.getTotalAmount()));
        log.info("Payment processed: invoiceId={} bookingId={} amount={}", invoice.getId(), bookingId, invoice.getTotalAmount());

        return toDto(invoice);
    }

    public InvoiceDto getByBookingId(Long bookingId) {
        log.info("Fetching invoice for bookingId={}", bookingId);
        return toDto(invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for bookingId: " + bookingId)));
    }

    public List<InvoiceDto> getByCustomerId(Long customerId) {
        log.info("Fetching invoices for customerId={}", customerId);
        return invoiceRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    private InvoiceDto toDto(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoice.getId());
        dto.setBookingId(invoice.getBookingId());
        dto.setCustomerId(invoice.getCustomerId());
        dto.setRoomAmount(invoice.getRoomAmount());
        dto.setAmenitiesAmount(invoice.getAmenitiesAmount());
        dto.setDiningAmount(invoice.getDiningAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        return dto;
    }
}

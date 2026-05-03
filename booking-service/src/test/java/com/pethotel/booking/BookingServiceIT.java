package com.pethotel.booking;

import com.pethotel.booking.client.RoomClient;
import com.pethotel.booking.dto.BookingDto;
import com.pethotel.booking.dto.BookingRequest;
import com.pethotel.booking.dto.RoomDto;
import com.pethotel.booking.entity.Booking;
import com.pethotel.booking.repository.BookingRepository;
import com.pethotel.booking.service.BookingService;
import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for BookingService with real PostgreSQL and embedded Kafka.
 * RoomClient (HTTP call to room-service) is mocked — we test our service logic,
 * not the remote service.
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1,
        topics = {"booking.created", "booking.confirmed", "booking.cancelled", "booking.completed"})
class BookingServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-booking-schema.sql");

    @MockBean RoomClient roomClient;

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;

    // ── create ────────────────────────────────────────────────────────────────────

    @Test
    void create_persistsBookingWithCorrectTotalPrice() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("4000"), RoomClass.ORDINARY));

        BookingDto result = bookingService.create(1L, request(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTotalPrice()).isEqualByComparingTo("8000");
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);

        Booking saved = bookingRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("8000");
        assertThat(saved.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void create_checkInAfterCheckOut_throwsAndDoesNotPersist() {
        long countBefore = bookingRepository.count();

        assertThatThrownBy(() -> bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(2))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bookingRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void create_roomNotFound_throwsNoSuchElement() {
        when(roomClient.getRoom(99L)).thenReturn(null);

        assertThatThrownBy(() -> bookingService.create(1L,
                request(99L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── confirm ───────────────────────────────────────────────────────────────────

    @Test
    void confirm_persistsStatusChange() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.MIDDLE));
        BookingDto created = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));

        BookingDto confirmed = bookingService.confirm(created.getId());

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirm_alreadyConfirmed_throwsIllegalState() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        bookingService.confirm(booking.getId());

        assertThatThrownBy(() -> bookingService.confirm(booking.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── cancel ────────────────────────────────────────────────────────────────────

    @Test
    void cancel_persistsCancelledStatus() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)));
        bookingService.confirm(booking.getId());

        bookingService.cancel(booking.getId(), 1L, false);

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_byDifferentCustomer_throwsAndLeavesStatusUnchanged() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(42L,
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)));

        assertThatThrownBy(() -> bookingService.cancel(booking.getId(), 99L, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PENDING);
    }

    // ── getMyBookings ─────────────────────────────────────────────────────────────

    @Test
    void getMyBookings_returnsOnlyBookingsForCustomer() {
        when(roomClient.getRoom(anyLong()))
                .thenReturn(roomDto(1L, new BigDecimal("2000"), RoomClass.ORDINARY));

        bookingService.create(10L, request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        bookingService.create(10L, request(1L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4)));
        bookingService.create(20L, request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(6)));

        List<BookingDto> customer10 = bookingService.getMyBookings(10L);
        List<BookingDto> customer20 = bookingService.getMyBookings(20L);

        assertThat(customer10).hasSize(2);
        assertThat(customer20).hasSize(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private BookingRequest request(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        BookingRequest req = new BookingRequest();
        req.setRoomId(roomId);
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        return req;
    }

    private RoomDto roomDto(Long id, BigDecimal price, RoomClass roomClass) {
        RoomDto dto = new RoomDto();
        dto.setId(id);
        dto.setPricePerNight(price);
        dto.setRoomClass(roomClass);
        return dto;
    }
}

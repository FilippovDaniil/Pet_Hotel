package com.pethotel.booking.service;

import com.pethotel.booking.client.RoomClient;
import com.pethotel.booking.dto.BookingDto;
import com.pethotel.booking.dto.BookingRequest;
import com.pethotel.booking.dto.RoomDto;
import com.pethotel.booking.entity.Booking;
import com.pethotel.booking.repository.BookingAmenityRepository;
import com.pethotel.booking.repository.BookingRepository;
import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.event.BookingCancelledEvent;
import com.pethotel.common.kafka.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingAmenityRepository amenityRepository;
    @Mock RoomClient roomClient;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock AmenityPriceCalculator priceCalculator;
    @InjectMocks BookingService bookingService;

    // ── create ───────────────────────────────────────────────────────────────────

    @Test
    void create_twoNightsAt5000_totalIs10000() {
        BookingRequest req = bookingRequest(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)); // 2 nights

        when(roomClient.getRoom(1L)).thenReturn(roomDto(new BigDecimal("5000"), RoomClass.ORDINARY));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingDto result = bookingService.create(1L, req);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("10000");
    }

    @Test
    void create_publishesBookingCreatedEvent() {
        BookingRequest req = bookingRequest(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        when(roomClient.getRoom(1L)).thenReturn(roomDto(new BigDecimal("3000"), RoomClass.ORDINARY));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        bookingService.create(1L, req);

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CREATED), any());
    }

    @Test
    void create_checkInAfterCheckOut_throwsIllegalArgument() {
        BookingRequest req = bookingRequest(1L,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkIn must be before checkOut");
    }

    @Test
    void create_checkInEqualsCheckOut_throwsIllegalArgument() {
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest req = bookingRequest(1L, date, date);

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_roomNotFound_throwsNoSuchElement() {
        BookingRequest req = bookingRequest(99L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(roomClient.getRoom(99L)).thenReturn(null);

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Room not found");
    }

    // ── confirm ──────────────────────────────────────────────────────────────────

    @Test
    void confirm_pendingBooking_changesStatusToConfirmed() {
        Booking booking = booking(1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CONFIRMED), any());
    }

    @Test
    void confirm_alreadyConfirmedBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void confirm_cancelledBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── cancel ───────────────────────────────────────────────────────────────────

    @Test
    void cancel_byReception_noPenaltyRegardlessOfDate() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now()); // within 24h → would normally trigger penalty
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        bookingService.cancel(1L, 99L, true);

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CANCELLED), captor.capture());
        assertThat(((BookingCancelledEvent) captor.getValue()).getPenaltyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancel_byCustomerWithin24h_chargesPenalty() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());          // today = within 24h
        booking.setCheckOutDate(LocalDate.now().plusDays(2));
        booking.setTotalPrice(new BigDecimal("10000"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        bookingService.cancel(1L, 1L, false);

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CANCELLED), captor.capture());
        assertThat(((BookingCancelledEvent) captor.getValue()).getPenaltyAmount())
                .isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void cancel_byCustomerFarInFuture_noPenalty() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(10)); // far future, no 24h penalty
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        booking.setTotalPrice(new BigDecimal("10000"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        bookingService.cancel(1L, 1L, false);

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CANCELLED), captor.capture());
        assertThat(((BookingCancelledEvent) captor.getValue()).getPenaltyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancel_byDifferentCustomer_throwsIllegalArgument() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 2L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    void cancel_completedBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 1L, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancel_alreadyCancelledBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 1L, false))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── checkIn ──────────────────────────────────────────────────────────────────

    @Test
    void checkIn_confirmedBooking_succeeds() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.checkIn(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void checkIn_pendingBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkIn(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");
    }

    // ── checkOut ─────────────────────────────────────────────────────────────────

    @Test
    void checkOut_setsStatusToCompleted_andPublishesEvent() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.checkOut(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_COMPLETED), any());
    }

    // ── getters ──────────────────────────────────────────────────────────────────

    @Test
    void getMyBookings_filtersById() {
        Booking b1 = booking(1L, BookingStatus.PENDING);
        Booking b2 = booking(2L, BookingStatus.CONFIRMED);
        when(bookingRepository.findByCustomerId(1L)).thenReturn(List.of(b1, b2));

        List<BookingDto> result = bookingService.getMyBookings(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getById_notFound_throwsNoSuchElement() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private BookingRequest bookingRequest(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        BookingRequest req = new BookingRequest();
        req.setRoomId(roomId);
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        return req;
    }

    private RoomDto roomDto(BigDecimal price, RoomClass roomClass) {
        RoomDto dto = new RoomDto();
        dto.setId(1L);
        dto.setPricePerNight(price);
        dto.setRoomClass(roomClass);
        return dto;
    }

    private Booking booking(Long id, BookingStatus status) {
        return Booking.builder()
                .id(id).customerId(1L).roomId(1L)
                .roomClass(RoomClass.ORDINARY)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .status(status)
                .totalPrice(new BigDecimal("10000"))
                .build();
    }
}

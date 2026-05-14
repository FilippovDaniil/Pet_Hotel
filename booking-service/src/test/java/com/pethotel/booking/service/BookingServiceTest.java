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

// Unit-тест BookingService: все зависимости замокированы (репозитории, Kafka, RoomClient).
// Изолирует бизнес-логику от инфраструктуры: тест работает без БД, Kafka и room-service.
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingAmenityRepository amenityRepository;
    @Mock RoomClient roomClient;          // HTTP-клиент к room-service
    @Mock KafkaTemplate<String, Object> kafkaTemplate; // Kafka producer
    @Mock AmenityPriceCalculator priceCalculator;
    @InjectMocks BookingService bookingService;

    // ── create ───────────────────────────────────────────────────────────────────

    // 2 ночи × 5000 руб/ночь = 10 000 руб.
    @Test
    void create_twoNightsAt5000_totalIs10000() {
        BookingRequest req = bookingRequest(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)); // 2 ночи

        when(roomClient.getRoom(1L)).thenReturn(roomDto(new BigDecimal("5000"), RoomClass.ORDINARY));
        // thenAnswer: имитируем присвоение id Hibernate при INSERT
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingDto result = bookingService.create(1L, req);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("10000");
    }

    // После создания бронирования Kafka должна получить событие BOOKING_CREATED.
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

        // verify: kafkaTemplate.send() был вызван с топиком BOOKING_CREATED и каким-то событием
        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CREATED), any());
    }

    // checkIn > checkOut → валидация отклоняет запрос до обращения к БД.
    @Test
    void create_checkInAfterCheckOut_throwsIllegalArgument() {
        BookingRequest req = bookingRequest(1L,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(2)); // неверный порядок

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkIn must be before checkOut");
    }

    // checkIn == checkOut → 0 ночей → тоже недопустимо.
    @Test
    void create_checkInEqualsCheckOut_throwsIllegalArgument() {
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest req = bookingRequest(1L, date, date); // 0 ночей

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // room-service вернул null (номер не найден) → NoSuchElementException.
    @Test
    void create_roomNotFound_throwsNoSuchElement() {
        BookingRequest req = bookingRequest(99L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(roomClient.getRoom(99L)).thenReturn(null); // симулируем 404 от room-service

        assertThatThrownBy(() -> bookingService.create(1L, req))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Room not found");
    }

    // ── confirm ──────────────────────────────────────────────────────────────────

    // PENDING → CONFIRMED + событие в Kafka.
    @Test
    void confirm_pendingBooking_changesStatusToConfirmed() {
        Booking booking = booking(1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CONFIRMED), any());
    }

    // Попытка подтвердить уже CONFIRMED → исключение (нарушение конечного автомата).
    @Test
    void confirm_alreadyConfirmedBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING"); // сообщение содержит требуемый статус
    }

    // CANCELLED нельзя подтвердить.
    @Test
    void confirm_cancelledBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── cancel ───────────────────────────────────────────────────────────────────

    // Ресепшн отменяет сегодняшний заезд: штраф не начисляется (isReception = true).
    @Test
    void cancel_byReception_noPenaltyRegardlessOfDate() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now()); // заезд сегодня — в пределах 24 ч
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ArgumentCaptor перехватывает объект события из kafkaTemplate.send()
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        bookingService.cancel(1L, 99L, true); // isReception = true → без штрафа

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CANCELLED), captor.capture());
        // Приводим к BookingCancelledEvent и проверяем поле penaltyAmount
        assertThat(((BookingCancelledEvent) captor.getValue()).getPenaltyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Клиент отменяет бронирование с заездом сегодня → штраф > 0.
    @Test
    void cancel_byCustomerWithin24h_chargesPenalty() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());           // заезд сегодня = в пределах 24 ч
        booking.setCheckOutDate(LocalDate.now().plusDays(2));
        booking.setTotalPrice(new BigDecimal("10000"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        bookingService.cancel(1L, 1L, false); // isReception = false → штраф применяется

        verify(kafkaTemplate).send(eq(KafkaTopics.BOOKING_CANCELLED), captor.capture());
        assertThat(((BookingCancelledEvent) captor.getValue()).getPenaltyAmount())
                .isGreaterThan(BigDecimal.ZERO); // штраф ненулевой
    }

    // Заезд через 10 дней → вне 24-часового окна → штраф 0.
    @Test
    void cancel_byCustomerFarInFuture_noPenalty() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(10)); // заезд далеко в будущем
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

    // Клиент пытается отменить чужое бронирование → IllegalArgumentException.
    // booking.customerId = 1, requestingUserId = 2 → не владелец.
    @Test
    void cancel_byDifferentCustomer_throwsIllegalArgument() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED); // customerId = 1
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 2L, false)) // userId = 2 — чужой
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized");
    }

    // COMPLETED нельзя отменить — заезд уже закончился.
    @Test
    void cancel_completedBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 1L, false))
                .isInstanceOf(IllegalStateException.class);
    }

    // CANCELLED нельзя отменить повторно.
    @Test
    void cancel_alreadyCancelledBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 1L, false))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── checkIn ──────────────────────────────────────────────────────────────────

    // checkIn не меняет статус (остаётся CONFIRMED) — только физическое заселение.
    @Test
    void checkIn_confirmedBooking_succeeds() {
        Booking booking = booking(1L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.checkIn(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED); // статус не меняется
    }

    // checkIn из PENDING → нельзя заселиться без подтверждения ресепшн.
    @Test
    void checkIn_pendingBooking_throwsIllegalState() {
        Booking booking = booking(1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkIn(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");
    }

    // ── checkOut ─────────────────────────────────────────────────────────────────

    // checkOut: CONFIRMED → COMPLETED + событие BOOKING_COMPLETED в Kafka.
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

    // getMyBookings: repository возвращает 2 бронирования для customerId=1 → оба в ответе.
    @Test
    void getMyBookings_filtersById() {
        Booking b1 = booking(1L, BookingStatus.PENDING);
        Booking b2 = booking(2L, BookingStatus.CONFIRMED);
        when(bookingRepository.findByCustomerId(1L)).thenReturn(List.of(b1, b2));

        List<BookingDto> result = bookingService.getMyBookings(1L);

        assertThat(result).hasSize(2);
    }

    // Несуществующее бронирование → NoSuchElementException с id в сообщении.
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

    // Заглушка Booking с customerId=1 (для тестов владения при отмене).
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

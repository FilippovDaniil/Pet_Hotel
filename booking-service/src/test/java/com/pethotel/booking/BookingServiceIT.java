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

// Интеграционный тест BookingService: реальная PostgreSQL (Testcontainers) + встроенный Kafka.
// RoomClient (@MockBean) изолирован — проверяем нашу логику, не удалённый room-service.
//
// @SpringBootTest — поднимает ПОЛНЫЙ Spring-контекст (все бины, все репозитории, Kafka).
//   В отличие от @WebMvcTest или @DataJpaTest — тест максимально приближён к продакшену.
// @EmbeddedKafka — встроенный Kafka-брокер (в памяти, без Docker).
//   Нужен потому, что BookingService.create/confirm/cancel/checkOut публикуют события в KafkaTemplate.
//   Без @EmbeddedKafka Spring Boot попытается подключиться к реальному Kafka и тест не запустится.
//   В customer-service Kafka не нужна (lazy connection), здесь — обязательна.
// topics — список топиков, которые @EmbeddedKafka создаст автоматически при старте.
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1,
        topics = {"booking.created", "booking.confirmed", "booking.cancelled", "booking.completed"})
class BookingServiceIT {

    // @Container — Testcontainers: контейнер создаётся один раз для всего класса (static).
    // @ServiceConnection — Spring Boot 3.1+: автоматически конфигурирует spring.datasource.*
    //   из адреса/порта запущенного контейнера (не нужно @DynamicPropertySource).
    // withInitScript — выполняет SQL-скрипт ПЕРЕД тем, как Hibernate делает DDL (CREATE TABLE).
    //   Создаёт схему "booking": без неё Hibernate упадёт с "schema not found".
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-booking-schema.sql");

    // @MockBean — заменяет бин RoomClient в Spring-контексте на мок Mockito.
    // RoomClient делает HTTP-вызов к room-service (WebClient.get().uri(...).retrieve()).
    // В IT-тесте room-service не запущен → нужен мок, иначе тест упадёт с ConnectException.
    @MockBean RoomClient roomClient;

    // @Autowired — инжектируем реальные (не мок) бины из поднятого Spring-контекста.
    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;

    // ── create ────────────────────────────────────────────────────────────────────

    // Основной happy-path: 2 ночи × 4000 = 8000 руб. Проверяем и DTO, и реальную запись в БД.
    @Test
    void create_persistsBookingWithCorrectTotalPrice() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("4000"), RoomClass.ORDINARY));

        BookingDto result = bookingService.create(1L, request(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3))); // 2 ночи

        assertThat(result.getId()).isNotNull(); // id назначен → запись попала в БД
        assertThat(result.getTotalPrice()).isEqualByComparingTo("8000"); // DTO содержит правильную цену
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING); // начальный статус

        // Дополнительная проверка через репозиторий: данные действительно сохранились в PostgreSQL.
        Booking saved = bookingRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("8000");
        assertThat(saved.getCustomerId()).isEqualTo(1L);
    }

    // checkIn > checkOut → валидация бросает исключение, транзакция не открывается → в БД ничего не записано.
    // countBefore/After — паттерн проверки "не было side effect в БД при ошибке".
    @Test
    void create_checkInAfterCheckOut_throwsAndDoesNotPersist() {
        long countBefore = bookingRepository.count(); // сколько записей было до теста

        assertThatThrownBy(() -> bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(2)))) // checkIn > checkOut
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bookingRepository.count()).isEqualTo(countBefore); // ничего не добавилось
    }

    // room-service вернул null → NoSuchElementException, запись не создана.
    @Test
    void create_roomNotFound_throwsNoSuchElement() {
        when(roomClient.getRoom(99L)).thenReturn(null); // симулируем "номер не существует"

        assertThatThrownBy(() -> bookingService.create(1L,
                request(99L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── confirm ───────────────────────────────────────────────────────────────────

    // PENDING → CONFIRMED: статус меняется и в DTO, и в реальной записи БД.
    @Test
    void confirm_persistsStatusChange() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.MIDDLE));
        BookingDto created = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));

        BookingDto confirmed = bookingService.confirm(created.getId());

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED); // DTO
        assertThat(bookingRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED); // реальная запись в PostgreSQL
    }

    // Повторный confirm → IllegalStateException (конечный автомат: CONFIRMED нельзя подтвердить ещё раз).
    @Test
    void confirm_alreadyConfirmed_throwsIllegalState() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        bookingService.confirm(booking.getId()); // первый confirm — успешно

        assertThatThrownBy(() -> bookingService.confirm(booking.getId())) // второй confirm — ошибка
                .isInstanceOf(IllegalStateException.class);
    }

    // ── cancel ────────────────────────────────────────────────────────────────────

    // Полный цикл create → confirm → cancel: итоговый статус в БД = CANCELLED.
    // checkIn через 5 дней — вне 24-часового окна → штраф 0.
    @Test
    void cancel_persistsCancelledStatus() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(1L,
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)));
        bookingService.confirm(booking.getId()); // сначала подтверждаем

        bookingService.cancel(booking.getId(), 1L, false); // отменяем (isReception=false, customerId=1)

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED); // статус сохранён в БД
    }

    // Клиент 99L пытается отменить бронирование клиента 42L → IllegalArgumentException.
    // Важно: статус в БД остаётся PENDING (откат транзакции / исключение до save).
    @Test
    void cancel_byDifferentCustomer_throwsAndLeavesStatusUnchanged() {
        when(roomClient.getRoom(1L)).thenReturn(roomDto(1L, new BigDecimal("3000"), RoomClass.ORDINARY));
        BookingDto booking = bookingService.create(42L, // создаёт клиент 42
                request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)));

        assertThatThrownBy(() -> bookingService.cancel(booking.getId(), 99L, false)) // пытается отменить клиент 99
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PENDING); // статус не изменился
    }

    // ── getMyBookings ─────────────────────────────────────────────────────────────

    // Фильтрация по customerId: 2 бронирования для клиента 10, 1 для клиента 20.
    // anyLong() — roomClient возвращает одинаковый roomDto для любого roomId.
    @Test
    void getMyBookings_returnsOnlyBookingsForCustomer() {
        when(roomClient.getRoom(anyLong()))
                .thenReturn(roomDto(1L, new BigDecimal("2000"), RoomClass.ORDINARY));

        bookingService.create(10L, request(1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        bookingService.create(10L, request(1L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4)));
        bookingService.create(20L, request(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(6)));

        List<BookingDto> customer10 = bookingService.getMyBookings(10L);
        List<BookingDto> customer20 = bookingService.getMyBookings(20L);

        assertThat(customer10).hasSize(2); // только свои 2 бронирования
        assertThat(customer20).hasSize(1); // только своё 1 бронирование
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    // Фабричный метод: создаёт BookingRequest с минимальным набором полей.
    private BookingRequest request(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        BookingRequest req = new BookingRequest();
        req.setRoomId(roomId);
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        return req;
    }

    // Фабричный метод: создаёт RoomDto — ответ, который возвращает замокированный RoomClient.
    private RoomDto roomDto(Long id, BigDecimal price, RoomClass roomClass) {
        RoomDto dto = new RoomDto();
        dto.setId(id);
        dto.setPricePerNight(price);
        dto.setRoomClass(roomClass);
        return dto;
    }
}

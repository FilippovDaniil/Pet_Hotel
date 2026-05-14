package com.pethotel.room.repository;

import com.pethotel.common.enums.RoomClass;
import com.pethotel.room.entity.Room;
import com.pethotel.room.entity.RoomUnavailableDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Интеграционный тест кастомного JPQL-запроса findAvailableRooms.
// Юнит-тестом этот запрос не проверить: его логика (NOT IN подзапрос) зависит от реальной БД.
//
// @DataJpaTest — поднимает только слой JPA (без web, без Kafka, без Redis).
//   Быстрее @SpringBootTest; запускает встроенный Hibernate DDL.
// @AutoConfigureTestDatabase(replace = NONE) — НЕ подменять PostgreSQL на H2.
//   Нам нужна реальная PostgreSQL: синтаксис NOT IN подзапроса идентичен, но лучше проверить с prod-БД.
// withInitScript — создаёт схему room ДО того, как Hibernate пытается делать DDL (CREATE TABLE).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RoomRepositoryIT {

    // static контейнер — создаётся один раз на весь класс, не пересоздаётся между тестами.
    @Container
    @ServiceConnection // автоматически настраивает spring.datasource.* из адреса контейнера
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-room-schema.sql"); // CREATE SCHEMA IF NOT EXISTS room

    @Autowired RoomRepository roomRepository;
    @Autowired RoomUnavailableDateRepository unavailableDateRepository;
    // TestEntityManager — тестовый аналог EntityManager: persistAndFlush() сохраняет И делает flush.
    // flush() нужен чтобы Hibernate записал данные в БД до выполнения следующего запроса.
    @Autowired TestEntityManager em;

    // Два номера создаются перед каждым тестом через @BeforeEach.
    // @Transactional на @DataJpaTest откатывает изменения после каждого теста → тесты изолированы.
    private Room room101;
    private Room room102;

    @BeforeEach
    void setUp() {
        // persistAndFlush = persist() + flush(): объект сохранён в БД с присвоенным id.
        room101 = em.persistAndFlush(Room.builder()
                .roomNumber("101").roomClass(RoomClass.ORDINARY)
                .capacity(2).pricePerNight(new BigDecimal("3000")).build());

        room102 = em.persistAndFlush(Room.builder()
                .roomNumber("102").roomClass(RoomClass.MIDDLE)
                .capacity(3).pricePerNight(new BigDecimal("5000")).build());
    }

    // ── findAvailableRooms ────────────────────────────────────────────────────────

    // Нет заблокированных дат → все номера с нужной вместимостью возвращаются.
    // extracting(Room::getRoomNumber) — AssertJ: извлекаем одно поле из коллекции для проверки.
    @Test
    void findAvailable_noUnavailableDates_returnsAllRoomsWithEnoughCapacity() {
        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .containsExactlyInAnyOrder("101", "102"); // порядок не важен
    }

    // Фильтр вместимости: guests=3, room101.capacity=2 → только room102 (capacity=3) проходит.
    @Test
    void findAvailable_capacityFilter_excludesSmallRooms() {
        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), 3);

        assertThat(result).extracting(Room::getRoomNumber)
                .containsOnly("102"); // room101 отсеян по capacity
    }

    // room101 заблокирован на запрашиваемые даты → его нет в результате; room102 есть.
    @Test
    void findAvailable_roomBlockedForRequestedDates_notReturned() {
        blockDate(room101, LocalDate.of(2025, 9, 10)); // 10 сентября занято
        blockDate(room101, LocalDate.of(2025, 9, 11)); // 11 сентября занято

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 12), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .doesNotContain("101") // заблокирован
                .contains("102");      // свободен
    }

    // Блокировка вне запрашиваемого диапазона не влияет на доступность.
    // room101 занят 5 сентября, но запрашиваем 10–12 сентября → room101 свободен.
    @Test
    void findAvailable_roomBlockedOnlyOutsideDates_stillReturned() {
        blockDate(room101, LocalDate.of(2025, 9, 5)); // блокировка за пределами запроса

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 12), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .contains("101"); // должен быть доступен
    }

    // checkOut — exclusive boundary: блокировка именно в день выезда не мешает бронированию.
    // Бронирование Aug 8–10 (выезд 10-го) совместимо с блокировкой 10-го (заезд следующего гостя).
    @Test
    void findAvailable_roomBlockedOnCheckOutDate_isAvailable() {
        blockDate(room101, LocalDate.of(2025, 8, 10)); // блокировка = дата выезда запроса

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 8), LocalDate.of(2025, 8, 10), 2); // checkOut = 10

        assertThat(result).extracting(Room::getRoomNumber)
                .contains("101"); // доступен, т.к. 10-е не входит в полуоткрытый интервал [8, 10)
    }

    // Все номера заблокированы → пустой результат.
    @Test
    void findAvailable_allRoomsBlocked_returnsEmpty() {
        blockDate(room101, LocalDate.of(2025, 10, 1));
        blockDate(room102, LocalDate.of(2025, 10, 1));

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 3), 1);

        assertThat(result).isEmpty();
    }

    // Полный цикл: заблокировали → разблокировали → номер снова доступен.
    // Проверяет deleteByRoomIdAndDateRange в реальной БД.
    @Test
    void blockDatesAndUnblock_roomBecomesAvailableAgain() {
        blockDate(room101, LocalDate.of(2025, 11, 1)); // блокируем 1 ноября

        // Удаляем блокировки в диапазоне октябрь–декабрь (включает 1 ноября)
        unavailableDateRepository.deleteByRoomIdAndDateRange(
                room101.getId(),
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 12, 1));
        em.flush(); // форсируем выполнение DELETE до следующего SELECT

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 3), 2);

        assertThat(result).extracting(Room::getRoomNumber).contains("101"); // снова доступен
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    // persistAndFlush: сохраняем дату и сразу отправляем SQL в БД.
    // Возвращаем объект на случай, если понадобится id или другие поля после сохранения.
    private RoomUnavailableDate blockDate(Room room, LocalDate date) {
        return em.persistAndFlush(
                RoomUnavailableDate.builder().room(room).date(date).build());
    }
}

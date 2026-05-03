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

/**
 * Integration tests for the custom JPQL query findAvailableRooms.
 * Uses real PostgreSQL via Testcontainers — the subquery filtering logic
 * is the main thing worth testing here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RoomRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-room-schema.sql");

    @Autowired RoomRepository roomRepository;
    @Autowired RoomUnavailableDateRepository unavailableDateRepository;
    @Autowired TestEntityManager em;

    private Room room101;
    private Room room102;

    @BeforeEach
    void setUp() {
        room101 = em.persistAndFlush(Room.builder()
                .roomNumber("101").roomClass(RoomClass.ORDINARY)
                .capacity(2).pricePerNight(new BigDecimal("3000")).build());

        room102 = em.persistAndFlush(Room.builder()
                .roomNumber("102").roomClass(RoomClass.MIDDLE)
                .capacity(3).pricePerNight(new BigDecimal("5000")).build());
    }

    // ── findAvailableRooms ────────────────────────────────────────────────────────

    @Test
    void findAvailable_noUnavailableDates_returnsAllRoomsWithEnoughCapacity() {
        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .containsExactlyInAnyOrder("101", "102");
    }

    @Test
    void findAvailable_capacityFilter_excludesSmallRooms() {
        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), 3);

        assertThat(result).extracting(Room::getRoomNumber)
                .containsOnly("102");
    }

    @Test
    void findAvailable_roomBlockedForRequestedDates_notReturned() {
        blockDate(room101, LocalDate.of(2025, 9, 10));
        blockDate(room101, LocalDate.of(2025, 9, 11));

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 12), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .doesNotContain("101")
                .contains("102");
    }

    @Test
    void findAvailable_roomBlockedOnlyOutsideDates_stillReturned() {
        // room101 is blocked Sep 5 — but we're querying Sep 10–12
        blockDate(room101, LocalDate.of(2025, 9, 5));

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 12), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .contains("101");
    }

    @Test
    void findAvailable_roomBlockedOnCheckOutDate_isAvailable() {
        // checkOut date is exclusive — a room blocked on Aug 10 is free for Aug 8–10 booking
        blockDate(room101, LocalDate.of(2025, 8, 10));

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 8, 8), LocalDate.of(2025, 8, 10), 2);

        assertThat(result).extracting(Room::getRoomNumber)
                .contains("101");
    }

    @Test
    void findAvailable_allRoomsBlocked_returnsEmpty() {
        blockDate(room101, LocalDate.of(2025, 10, 1));
        blockDate(room102, LocalDate.of(2025, 10, 1));

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 3), 1);

        assertThat(result).isEmpty();
    }

    @Test
    void blockDatesAndUnblock_roomBecomesAvailableAgain() {
        RoomUnavailableDate blocked = blockDate(room101, LocalDate.of(2025, 11, 1));

        unavailableDateRepository.deleteByRoomIdAndDateRange(
                room101.getId(),
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 12, 1));
        em.flush();

        List<Room> result = roomRepository.findAvailableRooms(
                LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 3), 2);

        assertThat(result).extracting(Room::getRoomNumber).contains("101");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private RoomUnavailableDate blockDate(Room room, LocalDate date) {
        return em.persistAndFlush(
                RoomUnavailableDate.builder().room(room).date(date).build());
    }
}

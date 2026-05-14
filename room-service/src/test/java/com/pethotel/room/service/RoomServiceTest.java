package com.pethotel.room.service;

import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.event.BookingCancelledEvent;
import com.pethotel.common.event.BookingCreatedEvent;
import com.pethotel.room.dto.RoomDto;
import com.pethotel.room.dto.RoomRequest;
import com.pethotel.room.entity.Room;
import com.pethotel.room.entity.RoomUnavailableDate;
import com.pethotel.room.repository.RoomRepository;
import com.pethotel.room.repository.RoomUnavailableDateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit-тест RoomService: все зависимости (репозитории) замокированы.
// @CacheEvict/@Cacheable на методах игнорируются без Spring-контекста — тест проверяет только логику.
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock RoomUnavailableDateRepository unavailableDateRepository;
    @InjectMocks RoomService roomService;

    // ── getAll / getById ─────────────────────────────────────────────────────────

    // getAll маппит список Entity → список DTO; roomNumber и порядок сохраняются.
    @Test
    void getAll_returnsMappedList() {
        when(roomRepository.findAll()).thenReturn(List.of(room(1L, "101"), room(2L, "102")));

        List<RoomDto> result = roomService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoomNumber()).isEqualTo("101");
        assertThat(result.get(1).getRoomNumber()).isEqualTo("102");
    }

    // getById находит номер по id и возвращает DTO с корректными полями.
    @Test
    void getById_returnsDto() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room(1L, "101")));

        RoomDto result = roomService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoomNumber()).isEqualTo("101");
    }

    // Несуществующий id → NoSuchElementException с id в сообщении → GlobalExceptionHandler → 404.
    @Test
    void getById_notFound_throwsNoSuchElement() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── create / update / delete ─────────────────────────────────────────────────

    // create: save() симулирует присвоение id (как делает Hibernate через SERIAL).
    @Test
    void create_savesAndReturnsDto() {
        when(roomRepository.save(any())).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(1L); // имитируем генерацию id БД
            return r;
        });

        RoomDto result = roomService.create(roomRequest("201", RoomClass.MIDDLE, 2, new BigDecimal("4000")));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoomNumber()).isEqualTo("201");
        assertThat(result.getRoomClass()).isEqualTo(RoomClass.MIDDLE);
    }

    // update: все поля перезаписываются из RoomRequest; BigDecimal сравниваем по значению.
    @Test
    void update_updatesFieldsAndReturnsDto() {
        Room existing = room(1L, "101");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        // save возвращает тот же объект — имитирует Hibernate "managed entity flush"
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoomDto result = roomService.update(1L,
                roomRequest("101A", RoomClass.PREMIUM, 3, new BigDecimal("7000")));

        assertThat(result.getRoomNumber()).isEqualTo("101A");
        assertThat(result.getRoomClass()).isEqualTo(RoomClass.PREMIUM);
        // isEqualByComparingTo: игнорирует scale (7000 == 7000.00 == 7.0E3)
        assertThat(result.getPricePerNight()).isEqualByComparingTo("7000");
    }

    // update несуществующего id → NoSuchElementException.
    @Test
    void update_notFound_throwsNoSuchElement() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(99L, roomRequest("X", RoomClass.ORDINARY, 1, BigDecimal.ONE)))
                .isInstanceOf(NoSuchElementException.class);
    }

    // delete: просто делегирует в репозиторий; cascade удалит unavailableDates через JPA.
    @Test
    void delete_callsRepository() {
        roomService.delete(1L);

        verify(roomRepository).deleteById(1L); // убеждаемся, что deleteById был вызван с нужным id
    }

    // ── blockDates ───────────────────────────────────────────────────────────────

    // blockDates: 1–3 июля = 3 ночи → saveAll должен получить список из 3 элементов.
    @Test
    void blockDates_savesOneEntryPerNight() {
        Room room = room(1L, "101");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .roomId(1L)
                .checkIn(LocalDate.of(2025, 7, 1))
                .checkOut(LocalDate.of(2025, 7, 4)) // checkOut exclusive → 3 nights: 1, 2, 3 июля
                .build();

        roomService.blockDates(event);

        // @SuppressWarnings("unchecked") — необходим для ArgumentCaptor с generic List<...>
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoomUnavailableDate>> captor = ArgumentCaptor.forClass(List.class);
        verify(unavailableDateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3); // ровно 3 строки в room_unavailable_dates
    }

    // Проверяем даты в сохранённых записях: первая = checkIn, вторая = checkIn+1.
    @Test
    void blockDates_eachEntryHasCorrectDate() {
        Room room = room(1L, "101");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        LocalDate checkIn = LocalDate.of(2025, 8, 10);
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .roomId(1L).checkIn(checkIn).checkOut(checkIn.plusDays(2)) // 2 ночи: 10, 11 августа
                .build();

        roomService.blockDates(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoomUnavailableDate>> captor = ArgumentCaptor.forClass(List.class);
        verify(unavailableDateRepository).saveAll(captor.capture());
        List<RoomUnavailableDate> saved = captor.getValue();
        assertThat(saved.get(0).getDate()).isEqualTo(checkIn);          // 10 августа
        assertThat(saved.get(1).getDate()).isEqualTo(checkIn.plusDays(1)); // 11 августа
    }

    // ── unblockDates ─────────────────────────────────────────────────────────────

    // unblockDates: вызывает deleteByRoomIdAndDateRange с правильным roomId.
    // any() для дат — мы не фиксируем точный диапазон, только что roomId = 5.
    @Test
    void unblockDates_callsDeleteByRoomIdAndDateRange() {
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .roomId(5L)
                .build();

        roomService.unblockDates(event);

        verify(unavailableDateRepository).deleteByRoomIdAndDateRange(eq(5L), any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Room room(Long id, String number) {
        return Room.builder()
                .id(id).roomNumber(number)
                .roomClass(RoomClass.ORDINARY)
                .capacity(2)
                .pricePerNight(new BigDecimal("3000"))
                .build();
    }

    private RoomRequest roomRequest(String number, RoomClass roomClass, int capacity, BigDecimal price) {
        RoomRequest req = new RoomRequest();
        req.setRoomNumber(number);
        req.setRoomClass(roomClass);
        req.setCapacity(capacity);
        req.setPricePerNight(price);
        req.setDescription("Test room");
        return req;
    }
}

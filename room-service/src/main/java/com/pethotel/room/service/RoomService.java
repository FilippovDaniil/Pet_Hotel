package com.pethotel.room.service;

import com.pethotel.common.event.BookingCancelledEvent;
import com.pethotel.common.event.BookingCreatedEvent;
import com.pethotel.room.dto.RoomDto;
import com.pethotel.room.dto.RoomRequest;
import com.pethotel.room.entity.Room;
import com.pethotel.room.entity.RoomUnavailableDate;
import com.pethotel.room.repository.RoomRepository;
import com.pethotel.room.repository.RoomUnavailableDateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomUnavailableDateRepository unavailableDateRepository;

    // @Cacheable — перед выполнением метода Spring проверяет Redis.
    //   Ключ кэша: "available-rooms::{checkIn}-{checkOut}-{guests}" (SpEL-выражение).
    //   Если запись в Redis есть (не истёк TTL 5 мин) — метод НЕ вызывается, результат берётся из кэша.
    //   Если нет — метод выполняется, результат сохраняется в Redis.
    //
    // TTL 5 минут — компромисс: допускаем незначительное устаревание данных ради снижения нагрузки на БД.
    // new ArrayList<>() — оборачиваем результат в изменяемый список, потому что
    //   .toList() возвращает неизменяемый список, а GenericJackson2JsonRedisSerializer
    //   при десериализации из Redis воссоздаёт конкретный тип — нам нужен предсказуемый ArrayList.
    @Cacheable(value = "available-rooms", key = "#checkIn + '-' + #checkOut + '-' + #guests")
    public List<RoomDto> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests) {
        log.info("Searching available rooms: checkIn={} checkOut={} guests={}", checkIn, checkOut, guests);
        return new ArrayList<>(roomRepository.findAvailableRooms(checkIn, checkOut, guests)
                .stream().map(this::toDto).toList());
    }

    public List<RoomDto> getAll() {
        return roomRepository.findAll().stream().map(this::toDto).toList();
    }

    public RoomDto getById(Long id) {
        return toDto(findRoom(id));
    }

    // @CacheEvict(allEntries = true) — при любом изменении номера инвалидируем весь кэш "available-rooms".
    // Это необходимо: если изменилась вместимость номера, старые кэшированные результаты поиска устарели.
    // allEntries = true — удаляем все ключи кэша, а не только один,
    //   потому что один номер может присутствовать в результатах множества разных запросов поиска.
    @Transactional
    @CacheEvict(value = "available-rooms", allEntries = true)
    public RoomDto create(RoomRequest request) {
        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomClass(request.getRoomClass())
                .capacity(request.getCapacity())
                .pricePerNight(request.getPricePerNight())
                .description(request.getDescription())
                .build();
        room = roomRepository.save(room);
        log.info("Room created: id={} number={}", room.getId(), room.getRoomNumber());
        return toDto(room);
    }

    @Transactional
    @CacheEvict(value = "available-rooms", allEntries = true)
    public RoomDto update(Long id, RoomRequest request) {
        Room room = findRoom(id);
        // Обновляем поля через сеттеры; @Transactional гарантирует автоматический flush в конце метода.
        // Hibernate отслеживает изменения в managed-сущности — save() здесь явный, но можно опустить.
        room.setRoomNumber(request.getRoomNumber());
        room.setRoomClass(request.getRoomClass());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setDescription(request.getDescription());
        log.info("Room updated: id={}", id);
        return toDto(roomRepository.save(room));
    }

    @Transactional
    @CacheEvict(value = "available-rooms", allEntries = true)
    public void delete(Long id) {
        // cascade = ALL на Room.unavailableDates → Hibernate автоматически удалит все даты номера.
        roomRepository.deleteById(id);
        log.info("Room deleted: id={}", id);
    }

    // Вызывается Kafka-consumer при получении события booking.created.
    // Создаём по одной строке RoomUnavailableDate на каждый день пребывания [checkIn, checkOut).
    @Transactional
    @CacheEvict(value = "available-rooms", allEntries = true)
    public void blockDates(BookingCreatedEvent event) {
        Room room = findRoom(event.getRoomId());
        List<RoomUnavailableDate> dates = new ArrayList<>();
        LocalDate date = event.getCheckIn();
        // Итерируем день за днём; checkOut — exclusive (последний день не блокируем).
        while (date.isBefore(event.getCheckOut())) {
            dates.add(RoomUnavailableDate.builder().room(room).date(date).build());
            date = date.plusDays(1);
        }
        // saveAll() — батч-операция: один PreparedStatement с несколькими VALUES, не N отдельных INSERT.
        unavailableDateRepository.saveAll(dates);
        log.info("Blocked dates for room {}: {} to {}", room.getId(), event.getCheckIn(), event.getCheckOut());
    }

    // Вызывается Kafka-consumer при получении события booking.cancelled.
    // Удаляем только будущие даты: прошедшие даты уже не влияют на поиск доступности.
    @Transactional
    @CacheEvict(value = "available-rooms", allEntries = true)
    public void unblockDates(BookingCancelledEvent event) {
        unavailableDateRepository.deleteByRoomIdAndDateRange(
                event.getRoomId(),
                LocalDate.now(),             // от сегодня (включительно)
                LocalDate.now().plusYears(2) // до практического предела горизонта бронирования
        );
        log.info("Unblocked dates for room {} due to cancellation", event.getRoomId());
    }

    // Вспомогательный метод: загружает Room или бросает 404-совместимое исключение.
    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + id));
    }

    // Entity → DTO: не передаём unavailableDates наружу — внутренняя деталь реализации.
    private RoomDto toDto(Room r) {
        RoomDto dto = new RoomDto();
        dto.setId(r.getId());
        dto.setRoomNumber(r.getRoomNumber());
        dto.setRoomClass(r.getRoomClass());
        dto.setCapacity(r.getCapacity());
        dto.setPricePerNight(r.getPricePerNight());
        dto.setDescription(r.getDescription());
        return dto;
    }
}

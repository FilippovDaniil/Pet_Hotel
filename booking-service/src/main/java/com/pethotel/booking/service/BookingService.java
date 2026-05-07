package com.pethotel.booking.service;

import com.pethotel.booking.client.RoomClient;
import com.pethotel.booking.dto.*;
import com.pethotel.booking.entity.Booking;
import com.pethotel.booking.entity.BookingAmenity;
import com.pethotel.booking.repository.BookingAmenityRepository;
import com.pethotel.booking.repository.BookingRepository;
import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.enums.ServiceType;
import com.pethotel.common.event.*;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingAmenityRepository amenityRepository;
    private final RoomClient roomClient;
    // KafkaTemplate<String, Object> — отправляет сообщения в Kafka.
    //   String — тип ключа сообщения (здесь ключ не используется, но тип нужен).
    //   Object — тип значения: позволяет отправлять разные классы событий через один template.
    //   Jackson сериализует объект в JSON; spring.json.add.type.headers = false — без type-заголовка.
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AmenityPriceCalculator priceCalculator;

    // Создание бронирования — самый сложный метод: 5 шагов.
    @Transactional
    public BookingDto create(Long customerId, BookingRequest request) {
        // Шаг 1: базовая валидация дат. checkIn должен быть строго раньше checkOut.
        if (!request.getCheckIn().isBefore(request.getCheckOut())) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }

        // Шаг 2: синхронный HTTP-вызов к room-service через WebClient.
        // Если room-service недоступен или номер не найден — block() выбросит RuntimeException → 500.
        RoomDto room = roomClient.getRoom(request.getRoomId());
        if (room == null) {
            throw new NoSuchElementException("Room not found: " + request.getRoomId());
        }

        // Шаг 3: рассчитываем стоимость проживания.
        // ChronoUnit.DAYS.between(start, end) — количество полных суток между датами.
        // Пример: checkIn=01.06, checkOut=05.06 → 4 ночи.
        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // Шаг 4: сохраняем бронирование без услуг (нужен id для @JoinColumn в BookingAmenity).
        // Первый save() нужен потому, что BookingAmenity.booking требует персистентного объекта Booking.
        // Статус PENDING — ждёт подтверждения ресепшн.
        Booking booking = Booking.builder()
                .customerId(customerId)
                .roomId(request.getRoomId())
                .roomClass(room.getRoomClass())
                .checkInDate(request.getCheckIn())
                .checkOutDate(request.getCheckOut())
                .status(BookingStatus.PENDING)
                .totalPrice(roomTotal) // промежуточная цена; перезапишется после расчёта услуг
                .build();
        booking = bookingRepository.save(booking);

        // Проверяем внутренние пересечения в запросе (два разных слота в одном запросе не должны перекрываться).
        validateNoInternalOverlaps(request.getAmenities());

        // Шаг 5: обрабатываем каждую услугу из запроса.
        BigDecimal amenitiesTotal = BigDecimal.ZERO;
        for (AmenityBookingRequest ar : request.getAmenities()) {
            // Проверяем: слот в пределах дат проживания и не занят другим клиентом.
            validateAmenitySlot(ar, booking);
            // Рассчитываем цену с учётом привилегий класса номера.
            // Важно передавать booking с уже добавленными услугами (для PREMIUM-квоты):
            // каждая следующая услуга "видит" предыдущие через booking.getAmenities().
            BigDecimal price = priceCalculator.calculatePrice(ar.getServiceType(), room.getRoomClass(), booking);
            BookingAmenity amenity = BookingAmenity.builder()
                    .booking(booking)
                    .serviceType(ar.getServiceType())
                    .startTime(ar.getStartTime())
                    .endTime(ar.getEndTime())
                    .price(price)
                    .build();
            // Добавляем в in-memory коллекцию — cascade ALL сохранит их вместе со вторым save().
            booking.getAmenities().add(amenity);
            amenitiesTotal = amenitiesTotal.add(price);
        }
        // Обновляем итоговую цену: проживание + все услуги.
        booking.setTotalPrice(roomTotal.add(amenitiesTotal));
        // Второй save() — сохраняет услуги (через cascade) и обновлённую totalPrice.
        booking = bookingRepository.save(booking);

        // Публикуем событие в Kafka: room-service заблокирует даты, billing-service создаст черновик счёта.
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(booking.getId())
                .customerId(customerId)
                .roomId(request.getRoomId())
                .roomClass(room.getRoomClass())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .totalPrice(booking.getTotalPrice())
                .build();
        kafkaTemplate.send(KafkaTopics.BOOKING_CREATED, event);
        log.info("Booking created: id={} customerId={}", booking.getId(), customerId);

        return toDto(booking);
    }

    // readOnly = true — Hibernate не отслеживает изменения сущностей (dirty checking отключён).
    // Чуть быстрее и экономнее для запросов без изменений.
    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(Long customerId) {
        return bookingRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getAll() {
        return bookingRepository.findAll().stream().map(this::toDto).toList();
    }

    public BookingDto getById(Long id) {
        return toDto(findBooking(id));
    }

    // Подтверждение: PENDING → CONFIRMED. Только ресепшн.
    @Transactional
    public BookingDto confirm(Long id) {
        Booking booking = findBooking(id);
        // Жёсткая проверка статусного перехода: confirm разрешён только из PENDING.
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING state");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        // Публикуем событие (пока нет потребителей, но топик зарезервирован для уведомлений).
        kafkaTemplate.send(KafkaTopics.BOOKING_CONFIRMED,
                BookingConfirmedEvent.builder()
                        .bookingId(id)
                        .customerId(booking.getCustomerId())
                        .build());
        log.info("Booking confirmed: id={}", id);
        return toDto(booking);
    }

    // Отмена: проверяет права, считает штраф, публикует событие.
    @Transactional
    public BookingDto cancel(Long bookingId, Long requestingUserId, boolean isReception) {
        Booking booking = findBooking(bookingId);

        // Проверка владения: клиент может отменить только своё бронирование.
        // Ресепшн и ADMIN (isReception = true) могут отменять любое.
        if (!isReception && !booking.getCustomerId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Not authorized to cancel this booking");
        }
        // Нельзя отменить уже отменённое или завершённое бронирование.
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        // Штраф за позднюю отмену: 30% если заезд сегодня или завтра (в пределах 24 ч).
        // Формула: LocalDate.now().plusDays(1).isAfter(checkInDate)
        //   ↔ checkInDate < now + 1 день
        //   ↔ checkInDate <= today (т.е. заезд сегодня или раньше)
        //   ↔ checkInDate == tomorrow (plusDays(1) → isAfter → true если checkIn == tomorrow)
        // Если isReception = true — штраф не применяется (административная отмена).
        BigDecimal penalty = BigDecimal.ZERO;
        if (!isReception) {
            boolean within24h = LocalDate.now().plusDays(1).isAfter(booking.getCheckInDate());
            if (within24h) {
                // Штраф = стоимость одной ночи (totalPrice / кол-во ночей).
                // Не 30% от totalPrice, а именно одна ночь — см. бизнес-требования.
                penalty = booking.getTotalPrice().divide(
                        BigDecimal.valueOf(ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate())));
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        // room-service получит событие и разблокирует даты номера.
        kafkaTemplate.send(KafkaTopics.BOOKING_CANCELLED,
                BookingCancelledEvent.builder()
                        .bookingId(bookingId)
                        .customerId(booking.getCustomerId())
                        .roomId(booking.getRoomId())
                        .penaltyAmount(penalty)
                        .build());
        log.info("Booking cancelled: id={} penalty={}", bookingId, penalty);
        return toDto(booking);
    }

    // checkIn: физическое заселение — статус НЕ меняется (остаётся CONFIRMED).
    // Метод существует для API-симметрии и логирования; реальная бизнес-логика при необходимости добавляется здесь.
    @Transactional
    public BookingDto checkIn(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking must be CONFIRMED to check in");
        }
        log.info("Booking checked-in: id={}", id);
        return toDto(booking);
    }

    // checkOut: выезд — CONFIRMED → COMPLETED + публикация события для billing-service.
    @Transactional
    public BookingDto checkOut(Long id) {
        Booking booking = findBooking(id);
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        // Считаем итог по услугам через stream().reduce():
        //   reduce(identity, accumulator) — складываем все price, начиная с ZERO.
        //   BigDecimal::add — method reference вместо лямбды (a, b) -> a.add(b).
        BigDecimal amenitiesTotal = booking.getAmenities().stream()
                .map(BookingAmenity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // roomTotal = totalPrice - amenitiesTotal (стоимость проживания без услуг).
        // billing-service ожидает раздельные суммы для детализации счёта.
        kafkaTemplate.send(KafkaTopics.BOOKING_COMPLETED,
                BookingCompletedEvent.builder()
                        .bookingId(id)
                        .customerId(booking.getCustomerId())
                        .roomId(booking.getRoomId())
                        .roomClass(booking.getRoomClass())
                        .checkIn(booking.getCheckInDate())
                        .checkOut(booking.getCheckOutDate())
                        .roomTotal(booking.getTotalPrice().subtract(amenitiesTotal))
                        .amenitiesTotal(amenitiesTotal)
                        .build());
        log.info("Booking checked-out: id={}", id);
        return toDto(booking);
    }

    // Проверяет, что в одном запросе нет перекрывающихся услуг.
    // O(n²) — допустимо: в одном запросе обычно не более 3–5 услуг.
    private void validateNoInternalOverlaps(List<AmenityBookingRequest> amenities) {
        for (int i = 0; i < amenities.size(); i++) {
            for (int j = i + 1; j < amenities.size(); j++) {
                AmenityBookingRequest a = amenities.get(i);
                AmenityBookingRequest b = amenities.get(j);
                // Классический предикат перекрытия интервалов: A.start < B.end && B.start < A.end.
                if (a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    throw new IllegalArgumentException(
                        "Услуги пересекаются по времени: " + a.getServiceType() + " и " + b.getServiceType());
                }
            }
        }
    }

    // Проверяет: слот услуги попадает в период проживания И не занят другим клиентом.
    private void validateAmenitySlot(AmenityBookingRequest request, Booking booking) {
        // toLocalDate() — срезаем время: сравниваем только даты.
        if (request.getStartTime().toLocalDate().isBefore(booking.getCheckInDate()) ||
            request.getEndTime().toLocalDate().isAfter(booking.getCheckOutDate())) {
            throw new IllegalArgumentException("Amenity booking must be within stay dates");
        }
        // Ищем пересечения со всеми существующими бронированиями этой услуги.
        List<BookingAmenity> conflicts = amenityRepository.findConflicting(
                request.getServiceType(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Time slot is already booked for " + request.getServiceType());
        }
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + id));
    }

    // Entity → DTO: вложенный маппинг включает список AmenityDto.
    private BookingDto toDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.setId(b.getId());
        dto.setCustomerId(b.getCustomerId());
        dto.setRoomId(b.getRoomId());
        dto.setRoomClass(b.getRoomClass());
        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setStatus(b.getStatus());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setCreatedAt(b.getCreatedAt());
        // Маппинг вложенного списка: для каждой BookingAmenity — свой AmenityDto.
        dto.setAmenities(b.getAmenities().stream().map(a -> {
            AmenityDto ad = new AmenityDto();
            ad.setId(a.getId());
            ad.setServiceType(a.getServiceType());
            ad.setStartTime(a.getStartTime());
            ad.setEndTime(a.getEndTime());
            ad.setPrice(a.getPrice());
            return ad;
        }).toList());
        return dto;
    }
}

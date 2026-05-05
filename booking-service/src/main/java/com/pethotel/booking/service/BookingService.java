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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AmenityPriceCalculator priceCalculator;

    @Transactional
    public BookingDto create(Long customerId, BookingRequest request) {
        if (!request.getCheckIn().isBefore(request.getCheckOut())) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }

        RoomDto room = roomClient.getRoom(request.getRoomId());
        if (room == null) {
            throw new NoSuchElementException("Room not found: " + request.getRoomId());
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .customerId(customerId)
                .roomId(request.getRoomId())
                .roomClass(room.getRoomClass())
                .checkInDate(request.getCheckIn())
                .checkOutDate(request.getCheckOut())
                .status(BookingStatus.PENDING)
                .totalPrice(roomTotal)
                .build();
        booking = bookingRepository.save(booking);

        validateNoInternalOverlaps(request.getAmenities());

        BigDecimal amenitiesTotal = BigDecimal.ZERO;
        for (AmenityBookingRequest ar : request.getAmenities()) {
            validateAmenitySlot(ar, booking);
            BigDecimal price = priceCalculator.calculatePrice(ar.getServiceType(), room.getRoomClass(), booking);
            BookingAmenity amenity = BookingAmenity.builder()
                    .booking(booking)
                    .serviceType(ar.getServiceType())
                    .startTime(ar.getStartTime())
                    .endTime(ar.getEndTime())
                    .price(price)
                    .build();
            booking.getAmenities().add(amenity);
            amenitiesTotal = amenitiesTotal.add(price);
        }
        booking.setTotalPrice(roomTotal.add(amenitiesTotal));
        booking = bookingRepository.save(booking);

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

    @Transactional
    public BookingDto confirm(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING state");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        kafkaTemplate.send(KafkaTopics.BOOKING_CONFIRMED,
                BookingConfirmedEvent.builder()
                        .bookingId(id)
                        .customerId(booking.getCustomerId())
                        .build());
        log.info("Booking confirmed: id={}", id);
        return toDto(booking);
    }

    @Transactional
    public BookingDto cancel(Long bookingId, Long requestingUserId, boolean isReception) {
        Booking booking = findBooking(bookingId);
        if (!isReception && !booking.getCustomerId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Not authorized to cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        BigDecimal penalty = BigDecimal.ZERO;
        if (!isReception) {
            boolean within24h = LocalDate.now().plusDays(1).isAfter(booking.getCheckInDate());
            if (within24h) {
                penalty = booking.getTotalPrice().divide(
                        BigDecimal.valueOf(ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate())));
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
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

    @Transactional
    public BookingDto checkIn(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking must be CONFIRMED to check in");
        }
        log.info("Booking checked-in: id={}", id);
        return toDto(booking);
    }

    @Transactional
    public BookingDto checkOut(Long id) {
        Booking booking = findBooking(id);
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        BigDecimal amenitiesTotal = booking.getAmenities().stream()
                .map(BookingAmenity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    private void validateNoInternalOverlaps(List<AmenityBookingRequest> amenities) {
        for (int i = 0; i < amenities.size(); i++) {
            for (int j = i + 1; j < amenities.size(); j++) {
                AmenityBookingRequest a = amenities.get(i);
                AmenityBookingRequest b = amenities.get(j);
                if (a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    throw new IllegalArgumentException(
                        "Услуги пересекаются по времени: " + a.getServiceType() + " и " + b.getServiceType());
                }
            }
        }
    }

    private void validateAmenitySlot(AmenityBookingRequest request, Booking booking) {
        if (request.getStartTime().toLocalDate().isBefore(booking.getCheckInDate()) ||
            request.getEndTime().toLocalDate().isAfter(booking.getCheckOutDate())) {
            throw new IllegalArgumentException("Amenity booking must be within stay dates");
        }
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

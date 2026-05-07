package com.pethotel.dining.service;

import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.event.OrderCreatedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import com.pethotel.dining.dto.OrderDto;
import com.pethotel.dining.dto.OrderRequest;
import com.pethotel.dining.entity.MenuItem;
import com.pethotel.dining.entity.Order;
import com.pethotel.dining.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuService menuService;
    private final DailyLimitService dailyLimitService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.booking-service.url:http://booking-service:8083}")
    private String bookingServiceUrl;

    @Transactional
    public OrderDto createOrder(Long customerId, OrderRequest request) {
        log.info("Creating order: customerId={} bookingId={} menuItemId={} quantity={}",
                customerId, request.getBookingId(), request.getMenuItemId(), request.getQuantity());

        MenuItem menuItem = menuService.findItem(request.getMenuItemId());
        if (!menuItem.isAvailable()) {
            throw new IllegalStateException("Menu item is not available: " + menuItem.getId());
        }

        BigDecimal totalAmount = menuItem.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        RoomClass roomClass = fetchRoomClass(request.getBookingId());
        BigDecimal dailyLimit = dailyLimitService.getDailyLimit(roomClass);
        BigDecimal alreadySpent = dailyLimitService.getDailySpent(request.getBookingId());

        BigDecimal paidByLimit;
        BigDecimal extraCharge;

        if (dailyLimit.compareTo(BigDecimal.ZERO) == 0) {
            paidByLimit = BigDecimal.ZERO;
            extraCharge = totalAmount;
        } else {
            BigDecimal remaining = dailyLimit.subtract(alreadySpent);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                paidByLimit = BigDecimal.ZERO;
                extraCharge = totalAmount;
            } else if (remaining.compareTo(totalAmount) >= 0) {
                paidByLimit = totalAmount;
                extraCharge = BigDecimal.ZERO;
            } else {
                paidByLimit = remaining;
                extraCharge = totalAmount.subtract(remaining);
            }
        }

        Order order = Order.builder()
                .bookingId(request.getBookingId())
                .customerId(customerId)
                .menuItemId(request.getMenuItemId())
                .menuItemName(menuItem.getName())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .paidByLimit(paidByLimit)
                .extraCharge(extraCharge)
                .deliveryType(request.getDeliveryType())
                .build();

        order = orderRepository.save(order);
        log.info("Order saved: id={} totalAmount={} paidByLimit={} extraCharge={}",
                order.getId(), totalAmount, paidByLimit, extraCharge);

        dailyLimitService.addSpending(request.getBookingId(), paidByLimit, LocalDate.now());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .bookingId(order.getBookingId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .paidByLimit(order.getPaidByLimit())
                .extraCharge(order.getExtraCharge())
                .orderTime(order.getOrderTime())
                .build();

        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(order.getBookingId()), event);
        log.info("Published order.created event: orderId={}", order.getId());

        return toDto(order);
    }

    public List<OrderDto> getByBookingId(Long bookingId) {
        log.info("Fetching orders for bookingId={}", bookingId);
        return orderRepository.findByBookingId(bookingId).stream().map(this::toDto).toList();
    }

    public List<OrderDto> getByCustomerId(Long customerId) {
        log.info("Fetching orders for customerId={}", customerId);
        return orderRepository.findByCustomerIdOrderByOrderTimeDesc(customerId).stream().map(this::toDto).toList();
    }

    private RoomClass fetchRoomClass(Long bookingId) {
        try {
            BookingResponse response = webClientBuilder.build()
                    .get()
                    .uri(bookingServiceUrl + "/api/bookings/" + bookingId)
                    .retrieve()
                    .bodyToMono(BookingResponse.class)
                    .block();
            if (response == null || response.getRoomClass() == null) {
                log.warn("No roomClass in booking response for bookingId={}, defaulting to ORDINARY", bookingId);
                return RoomClass.ORDINARY;
            }
            return response.getRoomClass();
        } catch (Exception e) {
            log.error("Failed to fetch roomClass for bookingId={}: {}", bookingId, e.getMessage());
            return RoomClass.ORDINARY;
        }
    }

    private OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setBookingId(order.getBookingId());
        dto.setCustomerId(order.getCustomerId());
        dto.setMenuItemId(order.getMenuItemId());
        dto.setMenuItemName(order.getMenuItemName());
        dto.setQuantity(order.getQuantity());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderTime(order.getOrderTime());
        dto.setPaidByLimit(order.getPaidByLimit());
        dto.setExtraCharge(order.getExtraCharge());
        dto.setDeliveryType(order.getDeliveryType());
        return dto;
    }

    // Inner DTO for WebClient response
    @lombok.Data
    public static class BookingResponse {
        private Long id;
        private RoomClass roomClass;
    }
}

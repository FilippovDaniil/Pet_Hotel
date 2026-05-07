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

    // Создание заказа — главный метод сервиса.
    // Шаги: проверка позиции → расчёт стоимости → получение класса номера → расчёт лимита → сохранение → Kafka.
    @Transactional
    public OrderDto createOrder(Long customerId, OrderRequest request) {
        log.info("Creating order: customerId={} bookingId={} menuItemId={} quantity={}",
                customerId, request.getBookingId(), request.getMenuItemId(), request.getQuantity());

        // Шаг 1: получаем позицию меню и проверяем доступность.
        MenuItem menuItem = menuService.findItem(request.getMenuItemId());
        if (!menuItem.isAvailable()) {
            throw new IllegalStateException("Menu item is not available: " + menuItem.getId());
        }

        // Шаг 2: полная стоимость заказа (без учёта лимита).
        BigDecimal totalAmount = menuItem.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Шаг 3: HTTP-запрос к booking-service для получения класса номера.
        // Класс номера нужен для определения дневного лимита (ORDINARY=0, MIDDLE=1000, PREMIUM=3000).
        // При ошибке запроса (booking-service недоступен) — fallback на ORDINARY (нет лимита).
        RoomClass roomClass = fetchRoomClass(request.getBookingId());
        BigDecimal dailyLimit  = dailyLimitService.getDailyLimit(roomClass);
        BigDecimal alreadySpent = dailyLimitService.getDailySpent(request.getBookingId());

        // Шаг 4: вычисляем разбивку paidByLimit / extraCharge.
        BigDecimal paidByLimit;
        BigDecimal extraCharge;

        if (dailyLimit.compareTo(BigDecimal.ZERO) == 0) {
            // ORDINARY: лимит = 0 → весь заказ за счёт клиента.
            paidByLimit = BigDecimal.ZERO;
            extraCharge = totalAmount;
        } else {
            BigDecimal remaining = dailyLimit.subtract(alreadySpent); // оставшийся лимит на сегодня
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                // Лимит исчерпан — весь заказ платит клиент.
                paidByLimit = BigDecimal.ZERO;
                extraCharge = totalAmount;
            } else if (remaining.compareTo(totalAmount) >= 0) {
                // Остатка лимита достаточно — заказ полностью покрыт.
                paidByLimit = totalAmount;
                extraCharge = BigDecimal.ZERO;
            } else {
                // Частичное покрытие: часть оплачивает лимит, остаток — клиент.
                paidByLimit = remaining;
                extraCharge = totalAmount.subtract(remaining);
            }
        }

        // Шаг 5: сохраняем заказ в PostgreSQL.
        Order order = Order.builder()
                .bookingId(request.getBookingId())
                .customerId(customerId)
                .menuItemId(request.getMenuItemId())
                .menuItemName(menuItem.getName()) // snapshot: имя фиксируется в момент заказа
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .paidByLimit(paidByLimit)
                .extraCharge(extraCharge)
                .deliveryType(request.getDeliveryType())
                .build();
        order = orderRepository.save(order);
        log.info("Order saved: id={} totalAmount={} paidByLimit={} extraCharge={}",
                order.getId(), totalAmount, paidByLimit, extraCharge);

        // Шаг 6: обновляем счётчик расходов в Redis (только paidByLimit — что реально потрачено из лимита).
        dailyLimitService.addSpending(request.getBookingId(), paidByLimit, LocalDate.now());

        // Шаг 7: публикуем событие в Kafka.
        // Ключ сообщения = bookingId (String): Kafka использует его для партиционирования.
        // Все события одного бронирования попадут в одну партицию → порядок гарантирован.
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

    // Получает класс номера через HTTP-вызов к booking-service.
    // Fallback на ORDINARY: если booking-service недоступен — лимит не применяется,
    // клиент платит полную стоимость. Это безопасная сторона: не даём бесплатную еду по ошибке.
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

    // Внутренний DTO для десериализации ответа booking-service через WebClient.
    // Аналог booking-service/dto/RoomDto в room-service — локальная копия нужных полей.
    // @lombok.Data — аннотация через FQN: класс вложенный, import lombok.Data уже есть выше.
    @lombok.Data
    public static class BookingResponse {
        private Long id;
        private RoomClass roomClass;  // только это поле нужно из всего BookingDto
    }
}

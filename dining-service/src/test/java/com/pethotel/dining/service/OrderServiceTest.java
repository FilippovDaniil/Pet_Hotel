package com.pethotel.dining.service;

import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.kafka.KafkaTopics;
import com.pethotel.dining.dto.OrderDto;
import com.pethotel.dining.dto.OrderRequest;
import com.pethotel.dining.entity.MenuItem;
import com.pethotel.dining.entity.Order;
import com.pethotel.dining.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock MenuService menuService;
    @Mock DailyLimitService dailyLimitService;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock WebClient.Builder webClientBuilder;
    @Mock WebClient webClient;
    @Mock WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock WebClient.ResponseSpec responseSpec;
    @InjectMocks OrderService orderService;

    @BeforeEach
    void setUp() {
        // lenient: some tests (unavailable item, getByBookingId) never reach WebClient
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ── limit split logic ────────────────────────────────────────────────────────

    @Test
    void createOrder_ordinaryRoom_zeroLimit_allExtraCharge() {
        stubRoomClass(RoomClass.ORDINARY);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("500"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.ORDINARY)).thenReturn(BigDecimal.ZERO);
        when(dailyLimitService.getDailySpent(1L)).thenReturn(BigDecimal.ZERO);
        stubSave();

        OrderDto result = orderService.createOrder(1L, orderRequest(1L, 1L, 2)); // total = 1000

        assertThat(result.getPaidByLimit()).isEqualByComparingTo("0");
        assertThat(result.getExtraCharge()).isEqualByComparingTo("1000");
    }

    @Test
    void createOrder_middleRoom_withinLimit_noExtraCharge() {
        stubRoomClass(RoomClass.MIDDLE);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("200"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.MIDDLE)).thenReturn(new BigDecimal("1000"));
        when(dailyLimitService.getDailySpent(1L)).thenReturn(new BigDecimal("600")); // remaining = 400
        stubSave();

        OrderDto result = orderService.createOrder(1L, orderRequest(1L, 1L, 1)); // total = 200

        assertThat(result.getPaidByLimit()).isEqualByComparingTo("200");
        assertThat(result.getExtraCharge()).isEqualByComparingTo("0");
    }

    @Test
    void createOrder_middleRoom_limitExhausted_fullExtraCharge() {
        stubRoomClass(RoomClass.MIDDLE);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("300"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.MIDDLE)).thenReturn(new BigDecimal("1000"));
        when(dailyLimitService.getDailySpent(1L)).thenReturn(new BigDecimal("1000")); // remaining = 0
        stubSave();

        OrderDto result = orderService.createOrder(1L, orderRequest(1L, 1L, 1));

        assertThat(result.getPaidByLimit()).isEqualByComparingTo("0");
        assertThat(result.getExtraCharge()).isEqualByComparingTo("300");
    }

    @Test
    void createOrder_middleRoom_partialLimit_splitCharge() {
        stubRoomClass(RoomClass.MIDDLE);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("400"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.MIDDLE)).thenReturn(new BigDecimal("1000"));
        when(dailyLimitService.getDailySpent(1L)).thenReturn(new BigDecimal("700")); // remaining = 300
        stubSave();

        OrderDto result = orderService.createOrder(1L, orderRequest(1L, 1L, 1)); // total = 400

        assertThat(result.getPaidByLimit()).isEqualByComparingTo("300");
        assertThat(result.getExtraCharge()).isEqualByComparingTo("100");
    }

    @Test
    void createOrder_premiumRoom_fullyCovered() {
        stubRoomClass(RoomClass.PREMIUM);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("1000"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.PREMIUM)).thenReturn(new BigDecimal("3000"));
        when(dailyLimitService.getDailySpent(1L)).thenReturn(BigDecimal.ZERO);
        stubSave();

        OrderDto result = orderService.createOrder(1L, orderRequest(1L, 1L, 1));

        assertThat(result.getPaidByLimit()).isEqualByComparingTo("1000");
        assertThat(result.getExtraCharge()).isEqualByComparingTo("0");
    }

    // ── validation ───────────────────────────────────────────────────────────────

    @Test
    void createOrder_unavailableItem_throwsIllegalState() {
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("200"), false));

        assertThatThrownBy(() -> orderService.createOrder(1L, orderRequest(1L, 1L, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    // ── events ───────────────────────────────────────────────────────────────────

    @Test
    void createOrder_publishesOrderCreatedEvent() {
        stubRoomClass(RoomClass.PREMIUM);
        when(menuService.findItem(1L)).thenReturn(menuItem(1L, new BigDecimal("100"), true));
        when(dailyLimitService.getDailyLimit(RoomClass.PREMIUM)).thenReturn(new BigDecimal("3000"));
        when(dailyLimitService.getDailySpent(1L)).thenReturn(BigDecimal.ZERO);
        stubSave();

        orderService.createOrder(1L, orderRequest(1L, 1L, 1));

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_CREATED), anyString(), any());
    }

    // ── getByBookingId ───────────────────────────────────────────────────────────

    @Test
    void getByBookingId_returnsMappedList() {
        when(orderRepository.findByBookingId(1L))
                .thenReturn(List.of(order(1L), order(2L)));

        List<OrderDto> result = orderService.getByBookingId(1L);

        assertThat(result).hasSize(2);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void stubRoomClass(RoomClass roomClass) {
        OrderService.BookingResponse response = new OrderService.BookingResponse();
        response.setRoomClass(roomClass);
        when(responseSpec.bodyToMono(OrderService.BookingResponse.class))
                .thenReturn(Mono.just(response));
    }

    private void stubSave() {
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
    }

    private OrderRequest orderRequest(Long bookingId, Long menuItemId, int quantity) {
        OrderRequest req = new OrderRequest();
        req.setBookingId(bookingId);
        req.setMenuItemId(menuItemId);
        req.setQuantity(quantity);
        return req;
    }

    private MenuItem menuItem(Long id, BigDecimal price, boolean available) {
        return MenuItem.builder()
                .id(id).name("Test Item").price(price)
                .category("food").available(available)
                .build();
    }

    private Order order(Long id) {
        return Order.builder()
                .id(id).bookingId(1L).customerId(1L).menuItemId(1L)
                .quantity(1).totalAmount(new BigDecimal("100"))
                .paidByLimit(new BigDecimal("100")).extraCharge(BigDecimal.ZERO)
                .build();
    }
}

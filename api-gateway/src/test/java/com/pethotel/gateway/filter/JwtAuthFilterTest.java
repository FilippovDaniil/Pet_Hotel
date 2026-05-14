package com.pethotel.gateway.filter;

import com.pethotel.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit-тест JwtAuthFilter: проверяет логику пропуска/блокировки запросов без реального Netty-сервера.
// MockServerWebExchange и MockServerHttpRequest — WebFlux-аналоги MockMvc из реактивного стека.
// .block() — синхронно ожидает завершения реактивного Mono (допустимо в тестах, не в prod-коде).
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtUtil jwtUtil;
    // GatewayFilterChain — следующий фильтр в цепочке; мокируем, чтобы не тянуть весь Gateway.
    @Mock GatewayFilterChain chain;
    @InjectMocks JwtAuthFilter filter;

    // ── public paths ─────────────────────────────────────────────────────────────

    // /api/auth/register — в PUBLIC_PATHS → фильтр пропускает, chain.filter() вызван, статус не выставлен.
    @Test
    void publicPath_register_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/auth/register");
        when(chain.filter(exchange)).thenReturn(Mono.empty()); // chain "выполнился успешно"

        filter.filter(exchange, chain).block(); // .block() синхронно ждёт Mono

        verify(chain).filter(exchange); // убеждаемся, что запрос передан дальше
        assertThat(exchange.getResponse().getStatusCode()).isNull(); // 401 не выставлен
    }

    // /api/auth/login — тоже в PUBLIC_PATHS.
    @Test
    void publicPath_login_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/auth/login");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    // /actuator/health — путь начинается с /actuator → isPublicPath() → true.
    @Test
    void publicPath_actuator_passesThrough() {
        MockServerWebExchange exchange = exchange("/actuator/health");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    // ── missing / malformed token ─────────────────────────────────────────────────

    // Нет заголовка Authorization → 401, chain.filter() не вызывается.
    @Test
    void missingAuthHeader_returns401() {
        MockServerWebExchange exchange = exchange("/api/bookings/1"); // защищённый путь

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // never() — убеждаемся, что запрос НЕ ушёл к downstream-сервису.
        verify(chain, never()).filter(any());
    }

    // Authorization: Basic ... — не Bearer → 401 (фильтр проверяет startsWith("Bearer ")).
    @Test
    void nonBearerToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bookings/1")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz") // Base64, не JWT
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // Bearer-токен есть, но jwtUtil.isValid() вернул false (истёк, подделан, неверная подпись) → 401.
    @Test
    void invalidJwt_returns401() {
        String token = "invalid.jwt.token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bookings/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(jwtUtil.isValid(token)).thenReturn(false); // mock возвращает "невалидный"

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── valid token ───────────────────────────────────────────────────────────────

    // Валидный токен: фильтр должен добавить X-User-Id и X-User-Role в мутированный запрос.
    @Test
    void validJwt_addsUserIdAndRoleHeaders() {
        String token = "valid.jwt.token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bookings/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(jwtUtil.isValid(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(42L);
        when(jwtUtil.getRole(token)).thenReturn("CUSTOMER");

        // ArgumentCaptor перехватывает аргумент, который был передан в chain.filter().
        // filter() передаёт МУТИРОВАННЫЙ exchange (с добавленными заголовками), а не оригинальный.
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // Проверяем заголовки в мутированном exchange, захваченном captor-ом.
        ServerWebExchange mutated = captor.getValue();
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("CUSTOMER");
    }

    // Валидный токен → 401 не выставляется, chain.filter() вызван (запрос прошёл).
    @Test
    void validJwt_doesNotReturn401() {
        String token = "valid.jwt.token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rooms/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(jwtUtil.isValid(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getRole(token)).thenReturn("ADMIN");
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // isNotEqualTo(UNAUTHORIZED) — статус либо null (не выставлен), либо 200 от chain.
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain).filter(any()); // chain был вызван ровно один раз
    }

    // ── public GET amenities (no auth needed) ────────────────────────────────────

    // GET /api/amenities — в PUBLIC_GET_PATHS → пропускается без токена.
    @Test
    void publicGet_amenitiesList_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/amenities"); // GET по умолчанию
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // GET /api/amenities/5/image — путь начинается с /api/amenities → тоже публичен.
    @Test
    void publicGet_amenityImage_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/amenities/5/image");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // POST /api/amenities (создание услуги) — не GET → не попадает в PUBLIC_GET_PATHS → 401.
    @Test
    void postToAmenities_withoutToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/amenities").build()); // POST, не GET

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // PUT /api/amenities/1 (обновление) — не GET → требует токен → 401.
    @Test
    void putToAmenities_withoutToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/amenities/1").build()); // PUT, не GET

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    // Вспомогательный метод: создаёт MockServerWebExchange для GET-запроса по указанному пути.
    // Используется когда заголовков не нужно — большинство тестов публичных путей.
    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}

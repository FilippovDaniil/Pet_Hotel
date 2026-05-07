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

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtUtil jwtUtil;
    @Mock GatewayFilterChain chain;
    @InjectMocks JwtAuthFilter filter;

    // ── public paths ─────────────────────────────────────────────────────────────

    @Test
    void publicPath_register_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/auth/register");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicPath_login_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/auth/login");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void publicPath_actuator_passesThrough() {
        MockServerWebExchange exchange = exchange("/actuator/health");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    // ── missing / malformed token ─────────────────────────────────────────────────

    @Test
    void missingAuthHeader_returns401() {
        MockServerWebExchange exchange = exchange("/api/bookings/1");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void nonBearerToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bookings/1")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidJwt_returns401() {
        String token = "invalid.jwt.token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bookings/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(jwtUtil.isValid(token)).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── valid token ───────────────────────────────────────────────────────────────

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

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange mutated = captor.getValue();
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("CUSTOMER");
    }

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

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain).filter(any());
    }

    // ── public GET amenities (no auth needed) ────────────────────────────────────

    @Test
    void publicGet_amenitiesList_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/amenities");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicGet_amenityImage_passesThrough() {
        MockServerWebExchange exchange = exchange("/api/amenities/5/image");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void postToAmenities_withoutToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/amenities").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void putToAmenities_withoutToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/amenities/1").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}

package com.pethotel.gateway.filter;

import com.pethotel.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

// Главный security-фильтр всего проекта. Выполняется до любого роутинга Spring Cloud Gateway.
// Алгоритм:
//   1. Пропустить публичные пути (auth, swagger, actuator) без проверки.
//   2. Пропустить GET /api/amenities без токена (публичный каталог услуг).
//   3. Для всех остальных: извлечь Bearer-токен → валидировать → добавить заголовки.
//   4. X-User-Id / X-User-Role / X-User-Email — downstream-сервисы читают из них контекст.
// GlobalFilter применяется ко всем маршрутам; Ordered(-100) гарантирует выполнение первым.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // Пути, которые пропускаются без JWT вне зависимости от метода
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    // GET-запросы к этим путям публичны (просмотр услуг без авторизации)
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/amenities"
    );

    private final JwtUtil jwtUtil;

    // Отрицательный приоритет: чем меньше число, тем раньше выполняется фильтр.
    // -100 гарантирует выполнение до любых других GlobalFilter (включая встроенные в gateway).
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // Публичные пути: пропускаем без проверки токена
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // GET /api/amenities: публичный просмотр каталога без авторизации
        if ("GET".equals(method) && PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // Извлекаем Authorization: Bearer <token>
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authentication failure: missing or malformed Authorization header for path {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();  // прерываем цепочку, ответ 401
        }

        String token = authHeader.substring(7);  // отрезаем "Bearer " (7 символов)

        // isValid() проверяет подпись и срок действия через JJWT; исключение → false
        if (!jwtUtil.isValid(token)) {
            log.warn("Authentication failure: invalid JWT token for path {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Извлекаем claims из валидного токена
        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        String email = jwtUtil.getEmail(token);

        // Мутируем запрос: добавляем заголовки — downstream-сервисы читают их как "авторизационный контекст"
        // exchange.mutate() создаёт новый неизменяемый объект с изменёнными полями
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .header("X-User-Email", email != null ? email : "")  // null-защита: email может отсутствовать
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);  // startsWith: /swagger-ui/index.html тоже проходит
    }

}

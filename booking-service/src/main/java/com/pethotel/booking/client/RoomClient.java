package com.pethotel.booking.client;

import com.pethotel.booking.dto.RoomDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// HTTP-клиент для межсервисного взаимодействия booking-service → room-service.
// Использует Spring WebClient (реактивный HTTP-клиент) в синхронном режиме через .block().
//
// Почему WebClient, а не RestTemplate?
//   RestTemplate устарел (deprecated в Spring 5). WebClient — его официальная замена.
//   Несмотря на реактивное API, block() позволяет использовать его синхронно в servlet-окружении.
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomClient {

    // WebClient.Builder инжектируется Spring Boot автоматически (auto-configured бин).
    // Используем Builder, а не готовый WebClient, чтобы каждый вызов мог конфигурировать baseUrl.
    private final WebClient.Builder webClientBuilder;

    // URL берётся из env ROOM_SERVICE_URL или из application.yml.
    // В Docker Compose сервис доступен по имени контейнера: http://room-service:8082.
    // При локальной разработке без Docker можно переопределить через переменную окружения.
    @Value("${services.room-service.url:http://room-service:8082}")
    private String roomServiceUrl;

    public RoomDto getRoom(Long roomId) {
        return webClientBuilder.build()
                // GET-запрос
                .get()
                // URI с подстановкой {id}: "http://room-service:8082/api/rooms/42"
                .uri(roomServiceUrl + "/api/rooms/{id}", roomId)
                // retrieve() — запускает запрос и переходит к обработке ответа.
                .retrieve()
                // bodyToMono(RoomDto.class) — десериализует JSON-тело ответа в Mono<RoomDto>.
                // Mono — реактивный контейнер для 0 или 1 элемента (аналог Optional в reactive).
                .bodyToMono(RoomDto.class)
                // doOnError — побочный эффект при ошибке: логируем, но не перехватываем исключение.
                .doOnError(e -> log.error("Failed to fetch room {}: {}", roomId, e.getMessage()))
                // block() — ждём завершения асинхронной операции и возвращаем результат синхронно.
                // В случае ошибки или таймаута — выбрасывает RuntimeException.
                // Возвращает null если room-service вернул 404 (bodyToMono → empty Mono → null).
                .block();
    }
}

package com.pethotel.booking.client;

import com.pethotel.booking.dto.RoomDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.room-service.url:http://room-service:8082}")
    private String roomServiceUrl;

    public RoomDto getRoom(Long roomId) {
        return webClientBuilder.build()
                .get()
                .uri(roomServiceUrl + "/api/rooms/{id}", roomId)
                .retrieve()
                .bodyToMono(RoomDto.class)
                .doOnError(e -> log.error("Failed to fetch room {}: {}", roomId, e.getMessage()))
                .block();
    }
}

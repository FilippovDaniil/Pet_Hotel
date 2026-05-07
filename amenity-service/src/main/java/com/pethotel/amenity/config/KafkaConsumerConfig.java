package com.pethotel.amenity.config;

import com.pethotel.common.event.ServiceBookedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka-потребитель для amenity-service.
// Подписан на топик service.booked — получает событие каждый раз, когда клиент бронирует услугу.
//
// Текущая реализация: только логирование.
// Это заглушка для будущей функциональности:
//   - ведение статистики загруженности слотов
//   - реальная блокировка временных слотов с проверкой доступности
//   - отправка подтверждений клиенту
//
// Такой подход ("consumer без логики") — нормальная практика в event-driven архитектуре:
// публикуем событие сейчас, а обработку добавляем по мере роста требований
// без изменения producer'а (booking-service).
@Slf4j
@Component
public class KafkaConsumerConfig {

    @KafkaListener(topics = KafkaTopics.SERVICE_BOOKED, groupId = "amenity-service")
    public void onServiceBooked(ServiceBookedEvent event) {
        // Логируем ключевые поля для диагностики: кто, что, за сколько.
        log.info("Kafka received service.booked: bookingId={} serviceType={} price={}",
                event.getBookingId(), event.getServiceType(), event.getPrice());
    }
}

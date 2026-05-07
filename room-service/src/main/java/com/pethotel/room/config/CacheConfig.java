package com.pethotel.room.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

// @EnableCaching — активирует Spring-механизм кэширования.
// Без этой аннотации @Cacheable и @CacheEvict в RoomService будут проигнорированы.
@Configuration
@EnableCaching
public class CacheConfig {

    // RedisCacheManager — реализация CacheManager для Redis; Spring подставляет его вместо
    // стандартного in-memory ConcurrentMapCacheManager.
    // RedisConnectionFactory — автоматически создаётся Spring Boot при наличии spring-data-redis
    // и настройках spring.redis.host/port в application.yml.
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // defaultCacheConfig() — базовый конфиг: по умолчанию значения хранятся как байты.
        // Переопределяем сериализатор на GenericJackson2JsonRedisSerializer:
        //   - сохраняет Java-объекты как JSON (читаемо в Redis CLI)
        //   - включает _class поле для точного восстановления типа при десериализации
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                // Именованные кэши с индивидуальными TTL:
                .withInitialCacheConfigurations(Map.of(
                    // "available-rooms" — результаты поиска свободных номеров: TTL 5 минут.
                    // Короткий TTL: номера бронируются и освобождаются часто.
                    "available-rooms", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                    // "room-prices" — цены меняются редко: TTL 1 час.
                    "room-prices",     defaultConfig.entryTtl(Duration.ofHours(1))
                ))
                // Для всех остальных кэшей (если вдруг добавятся @Cacheable с другим именем):
                // применяем тот же сериализатор, TTL 5 минут.
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}

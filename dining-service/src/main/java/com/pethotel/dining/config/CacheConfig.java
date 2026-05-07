package com.pethotel.dining.config;

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

// Конфигурация Redis-кэша для dining-service.
// Используется для @Cacheable("menu-items") в MenuService.
//
// Внимание: dining-service использует Redis двояко:
//   1. RedisCacheManager (этот класс) — для Spring Cache (@Cacheable) — кэш меню, TTL 1 час.
//   2. StringRedisTemplate (DailyLimitService) — прямой доступ к Redis — счётчики лимитов, TTL до полуночи.
// Это два разных механизма одного Redis-соединения; ключи не пересекаются (разные пространства имён).
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(Map.of(
                    // Меню меняется редко — TTL 1 час. @CacheEvict сбрасывает при каждом изменении.
                    "menu-items", defaultConfig.entryTtl(Duration.ofHours(1))
                ))
                // Остальные именованные кэши (если появятся) — TTL 30 минут.
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .build();
    }
}

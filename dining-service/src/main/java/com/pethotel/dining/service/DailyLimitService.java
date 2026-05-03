package com.pethotel.dining.service;

import com.pethotel.common.enums.RoomClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final BigDecimal LIMIT_ORDINARY = BigDecimal.ZERO;
    private static final BigDecimal LIMIT_MIDDLE   = new BigDecimal("1000");
    private static final BigDecimal LIMIT_PREMIUM  = new BigDecimal("3000");

    public BigDecimal getDailySpent(Long bookingId) {
        String key = buildKey(bookingId, LocalDate.now());
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid daily spending value in Redis for key={}: {}", key, value);
            return BigDecimal.ZERO;
        }
    }

    public void addSpending(Long bookingId, BigDecimal amount, LocalDate date) {
        String key = buildKey(bookingId, date);
        String current = stringRedisTemplate.opsForValue().get(key);
        BigDecimal currentAmount = (current != null) ? new BigDecimal(current) : BigDecimal.ZERO;
        BigDecimal newAmount = currentAmount.add(amount);
        Duration ttl = computeTtlToMidnight(date);
        stringRedisTemplate.opsForValue().set(key, newAmount.toPlainString(), ttl);
        log.info("Updated daily spending: bookingId={} date={} spent={}", bookingId, date, newAmount);
    }

    public BigDecimal getDailyLimit(RoomClass roomClass) {
        return switch (roomClass) {
            case ORDINARY -> LIMIT_ORDINARY;
            case MIDDLE   -> LIMIT_MIDDLE;
            case PREMIUM  -> LIMIT_PREMIUM;
        };
    }

    private String buildKey(Long bookingId, LocalDate date) {
        return "dining:limit:" + bookingId + ":" + date;
    }

    private Duration computeTtlToMidnight(LocalDate date) {
        LocalDateTime midnight = date.plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration duration = Duration.between(LocalDateTime.now(), midnight);
        return duration.isNegative() ? Duration.ofSeconds(60) : duration;
    }
}

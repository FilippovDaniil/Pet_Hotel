package com.pethotel.dining.service;

import com.pethotel.common.enums.RoomClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyLimitServiceTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks DailyLimitService dailyLimitService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── getDailyLimit ────────────────────────────────────────────────────────────

    @Test void getDailyLimit_ordinary_returnsZero() {
        assertThat(dailyLimitService.getDailyLimit(RoomClass.ORDINARY)).isEqualByComparingTo("0");
    }

    @Test void getDailyLimit_middle_returns1000() {
        assertThat(dailyLimitService.getDailyLimit(RoomClass.MIDDLE)).isEqualByComparingTo("1000");
    }

    @Test void getDailyLimit_premium_returns3000() {
        assertThat(dailyLimitService.getDailyLimit(RoomClass.PREMIUM)).isEqualByComparingTo("3000");
    }

    // ── getDailySpent ────────────────────────────────────────────────────────────

    @Test
    void getDailySpent_nullInRedis_returnsZero() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("0");
    }

    @Test
    void getDailySpent_validValue_returnsParsed() {
        when(valueOps.get(anyString())).thenReturn("750.00");

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("750");
    }

    @Test
    void getDailySpent_invalidValue_returnsZero() {
        when(valueOps.get(anyString())).thenReturn("not-a-number");

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("0");
    }

    // ── addSpending ──────────────────────────────────────────────────────────────

    @Test
    void addSpending_usesCorrectKeyFormat() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        when(valueOps.get("dining:limit:42:2025-06-15")).thenReturn(null);

        dailyLimitService.addSpending(42L, new BigDecimal("100"), date);

        verify(valueOps).set(eq("dining:limit:42:2025-06-15"), eq("100"), any(Duration.class));
    }

    @Test
    void addSpending_accumulatesExistingValue() {
        LocalDate date = LocalDate.now();
        String key = "dining:limit:1:" + date;
        when(valueOps.get(key)).thenReturn("200.00");

        dailyLimitService.addSpending(1L, new BigDecimal("150"), date);

        verify(valueOps).set(eq(key), eq("350.00"), any(Duration.class));
    }

    @Test
    void addSpending_noExistingValue_storesAmount() {
        when(valueOps.get(anyString())).thenReturn(null);

        dailyLimitService.addSpending(1L, new BigDecimal("500"), LocalDate.now());

        verify(valueOps).set(anyString(), eq("500"), any(Duration.class));
    }
}

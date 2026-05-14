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

// Unit-тест DailyLimitService — проверяет логику лимитов буфета без Spring-контекста.
// StringRedisTemplate и ValueOperations замокированы: тест не требует запущенного Redis.
@ExtendWith(MockitoExtension.class)
class DailyLimitServiceTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    // ValueOperations<String, String> — интерфейс для команд Redis типа GET/SET.
    // stringRedisTemplate.opsForValue() возвращает его; мокируем оба.
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks DailyLimitService dailyLimitService;

    // lenient(): opsForValue() не вызывается в каждом тесте (например, getDailyLimit не трогает Redis).
    // Без lenient() Mockito выдаёт UnnecessaryStubbingException для тестов, которые не дойдут до этого стаба.
    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── getDailyLimit ────────────────────────────────────────────────────────────
    // Таблица лимитов: ORDINARY=0, MIDDLE=1000, PREMIUM=3000.
    // isEqualByComparingTo: BigDecimal-сравнение без учёта scale (0 == 0.0 == 0.00).

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

    // Redis не содержит ключа (гость только зашёл) → 0 уже потрачено.
    @Test
    void getDailySpent_nullInRedis_returnsZero() {
        when(valueOps.get(anyString())).thenReturn(null); // ключ отсутствует в Redis

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("0");
    }

    // Redis содержит "750.00" → BigDecimal("750.00") → isEqualByComparingTo("750") проходит.
    @Test
    void getDailySpent_validValue_returnsParsed() {
        when(valueOps.get(anyString())).thenReturn("750.00");

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("750");
    }

    // Повреждённое значение в Redis (не число) → безопасный fallback к 0 вместо NumberFormatException.
    @Test
    void getDailySpent_invalidValue_returnsZero() {
        when(valueOps.get(anyString())).thenReturn("not-a-number");

        assertThat(dailyLimitService.getDailySpent(1L)).isEqualByComparingTo("0");
    }

    // ── addSpending ──────────────────────────────────────────────────────────────

    // Ключ строится как "dining:limit:{bookingId}:{date}".
    // Формат даты ISO: "2025-06-15" → ключ уникален для каждого дня.
    @Test
    void addSpending_usesCorrectKeyFormat() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        when(valueOps.get("dining:limit:42:2025-06-15")).thenReturn(null); // ещё не тратил

        dailyLimitService.addSpending(42L, new BigDecimal("100"), date);

        // Проверяем: set() был вызван с правильным ключом, суммой и каким-то TTL (Duration).
        verify(valueOps).set(eq("dining:limit:42:2025-06-15"), eq("100"), any(Duration.class));
    }

    // Накопление: в Redis уже "200.00", добавляем 150 → должно стать "350.00".
    // toPlainString(): сохраняем "350.00", а не "3.5E+2" (StringRedisTemplate — строки).
    @Test
    void addSpending_accumulatesExistingValue() {
        LocalDate date = LocalDate.now();
        String key = "dining:limit:1:" + date;
        when(valueOps.get(key)).thenReturn("200.00");

        dailyLimitService.addSpending(1L, new BigDecimal("150"), date);

        verify(valueOps).set(eq(key), eq("350.00"), any(Duration.class));
    }

    // Первая трата за день: ключа нет → записываем сумму напрямую.
    @Test
    void addSpending_noExistingValue_storesAmount() {
        when(valueOps.get(anyString())).thenReturn(null);

        dailyLimitService.addSpending(1L, new BigDecimal("500"), LocalDate.now());

        verify(valueOps).set(anyString(), eq("500"), any(Duration.class));
    }
}

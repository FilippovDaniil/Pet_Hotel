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

// Управляет дневным лимитом расходов на буфет для каждого бронирования.
// Лимит хранится в Redis (а не в PostgreSQL) по двум причинам:
//   1. TTL: запись автоматически удаляется в полночь — не нужно писать cleanup-задачи.
//   2. Быстрый инкремент: Redis атомарен по умолчанию, не нужны транзакции БД для счётчика.
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitService {

    // StringRedisTemplate — специализированная версия RedisTemplate<String, String>.
    // Хранит значения как строки: числа сохраняем через BigDecimal.toPlainString(), читаем через new BigDecimal(String).
    // Альтернатива — RedisTemplate<String, BigDecimal>, но StringRedisTemplate проще в конфигурации.
    private final StringRedisTemplate stringRedisTemplate;

    // Дневные лимиты расходов на буфет по классу номера:
    private static final BigDecimal LIMIT_ORDINARY = BigDecimal.ZERO;           // без лимита — всё за счёт клиента
    private static final BigDecimal LIMIT_MIDDLE   = new BigDecimal("1000");    // 1000 руб/день включено
    private static final BigDecimal LIMIT_PREMIUM  = new BigDecimal("3000");    // 3000 руб/день включено

    // Возвращает суммарные расходы за сегодня для данного бронирования.
    // Используется перед каждым заказом для расчёта оставшегося лимита.
    public BigDecimal getDailySpent(Long bookingId) {
        String key = buildKey(bookingId, LocalDate.now());
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            // Ключа нет → первый заказ за день, расходов ещё не было.
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // Защита от повреждённых данных в Redis.
            log.warn("Invalid daily spending value in Redis for key={}: {}", key, value);
            return BigDecimal.ZERO;
        }
    }

    // Добавляет сумму к счётчику расходов за день.
    // Вызывается после успешного сохранения заказа; amount = paidByLimit (часть, покрытая лимитом).
    // Важно: добавляем только paidByLimit, а не totalAmount — extraCharge идёт в счёт отдельно.
    public void addSpending(Long bookingId, BigDecimal amount, LocalDate date) {
        String key = buildKey(bookingId, date);
        String current = stringRedisTemplate.opsForValue().get(key);
        BigDecimal currentAmount = (current != null) ? new BigDecimal(current) : BigDecimal.ZERO;
        BigDecimal newAmount = currentAmount.add(amount);
        // TTL до полуночи: ключ автоматически удалится в начале следующего дня.
        Duration ttl = computeTtlToMidnight(date);
        // toPlainString() — формат без экспоненты: "1250.00", а не "1.25E+3".
        stringRedisTemplate.opsForValue().set(key, newAmount.toPlainString(), ttl);
        log.info("Updated daily spending: bookingId={} date={} spent={}", bookingId, date, newAmount);
    }

    // Возвращает дневной лимит для заданного класса номера.
    public BigDecimal getDailyLimit(RoomClass roomClass) {
        return switch (roomClass) {
            case ORDINARY -> LIMIT_ORDINARY;
            case MIDDLE   -> LIMIT_MIDDLE;
            case PREMIUM  -> LIMIT_PREMIUM;
        };
    }

    // Ключ в Redis: "dining:limit:{bookingId}:{date}"
    // Пример: "dining:limit:42:2025-06-15"
    // Пространство имён (dining:limit:) защищает от коллизий с другими ключами в общем Redis.
    private String buildKey(Long bookingId, LocalDate date) {
        return "dining:limit:" + bookingId + ":" + date;
    }

    // Рассчитывает TTL от текущего момента до полуночи (начала следующего дня).
    // Если почему-то вычисленная длительность отрицательна (например, запрос в 23:59:59) —
    // ставим минимальный TTL 60 секунд, чтобы ключ всё равно удалился.
    private Duration computeTtlToMidnight(LocalDate date) {
        LocalDateTime midnight = date.plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration duration = Duration.between(LocalDateTime.now(), midnight);
        return duration.isNegative() ? Duration.ofSeconds(60) : duration;
    }
}

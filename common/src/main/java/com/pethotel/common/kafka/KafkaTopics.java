package com.pethotel.common.kafka;

// Единый реестр имён Kafka-топиков для всего проекта.
//
// Зачем выносить в common:
//   Producer и consumer должны использовать одно и то же имя строки.
//   Если каждый сервис хранит своё магическое значение "booking.created",
//   опечатка в одном месте разорвёт связь между сервисами.
//   Константы из общей библиотеки исключают дублирование и делают опечатку compile-time ошибкой.
//
// Соглашение об именах: <domain>.<event> — читаемо и легко фильтруется в Kafka UI.
//
// final + приватный конструктор — утилитный класс, не предназначен для наследования и создания экземпляров.
public final class KafkaTopics {

    // Запрет создания экземпляра: KafkaTopics.BOOKING_CREATED — ок, new KafkaTopics() — compile error.
    private KafkaTopics() {}

    // booking-service → room-service (блокирует даты), billing-service (создаёт черновик счёта)
    public static final String BOOKING_CREATED   = "booking.created";

    // booking-service → (сейчас нет активных потребителей; зарезервировано для уведомлений)
    public static final String BOOKING_CONFIRMED = "booking.confirmed";

    // booking-service → room-service (разблокирует даты номера)
    public static final String BOOKING_CANCELLED = "booking.cancelled";

    // booking-service → billing-service (финализирует счёт: стоимость номера + услуги)
    public static final String BOOKING_COMPLETED = "booking.completed";

    // billing-service → (зарезервировано; потребителей пока нет)
    public static final String PAYMENT_PROCESSED = "payment.processed";

    // dining-service → billing-service (добавляет стоимость заказа к открытому счёту)
    public static final String ORDER_CREATED     = "order.created";

    // booking-service → amenity-service (учитывает загруженность услуги на конкретный слот)
    public static final String SERVICE_BOOKED    = "service.booked";
}

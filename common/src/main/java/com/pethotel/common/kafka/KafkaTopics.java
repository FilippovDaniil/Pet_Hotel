package com.pethotel.common.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String BOOKING_CREATED   = "booking.created";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKING_COMPLETED = "booking.completed";
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String ORDER_CREATED     = "order.created";
    public static final String SERVICE_BOOKED    = "service.booked";
}

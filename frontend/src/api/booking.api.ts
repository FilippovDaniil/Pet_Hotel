// API-клиент для booking-service: создание и управление бронированиями.
import client from './client'
import type { Booking, BookingRequest } from '../types'

export const bookingApi = {
  // POST /api/bookings — создать новое бронирование (CUSTOMER).
  // Тело включает roomId, checkIn, checkOut и массив amenities (может быть пустым).
  create: (data: BookingRequest) =>
    client.post<Booking>('/bookings', data).then(r => r.data),

  // GET /api/bookings/my — бронирования текущего пользователя (CUSTOMER).
  // Сервер фильтрует по X-User-Id заголовку от gateway.
  getMyBookings: () =>
    client.get<Booking[]>('/bookings/my').then(r => r.data),

  // GET /api/bookings/all — все бронирования системы (RECEPTION/ADMIN).
  getAll: () =>
    client.get<Booking[]>('/bookings/all').then(r => r.data),

  // GET /api/bookings/{id} — детали одного бронирования (все роли).
  getById: (id: number) =>
    client.get<Booking>(`/bookings/${id}`).then(r => r.data),

  // POST /api/bookings/{id}/cancel — отменить бронирование (CUSTOMER/RECEPTION).
  // При отмене клиентом менее чем за 24 ч до заезда применяется штраф 30%.
  cancel: (id: number) =>
    client.post<Booking>(`/bookings/${id}/cancel`).then(r => r.data),

  // POST /api/bookings/{id}/confirm — подтвердить бронирование PENDING→CONFIRMED (RECEPTION).
  confirm: (id: number) =>
    client.post<Booking>(`/bookings/${id}/confirm`).then(r => r.data),

  // POST /api/bookings/{id}/checkin — зафиксировать заезд (RECEPTION).
  // Допустим только из статуса CONFIRMED.
  checkIn: (id: number) =>
    client.post<Booking>(`/bookings/${id}/checkin`).then(r => r.data),

  // POST /api/bookings/{id}/checkout — зафиксировать выезд CONFIRMED→COMPLETED (RECEPTION).
  // После этого billing-service создаёт финальный счёт через Kafka booking.completed.
  checkOut: (id: number) =>
    client.post<Booking>(`/bookings/${id}/checkout`).then(r => r.data),
}

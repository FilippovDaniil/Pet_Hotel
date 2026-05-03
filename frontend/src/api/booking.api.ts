import client from './client'
import type { Booking, BookingRequest } from '../types'

export const bookingApi = {
  create: (data: BookingRequest) =>
    client.post<Booking>('/bookings', data).then(r => r.data),

  getMyBookings: () =>
    client.get<Booking[]>('/bookings/my').then(r => r.data),

  getAll: () =>
    client.get<Booking[]>('/bookings/all').then(r => r.data),

  getById: (id: number) =>
    client.get<Booking>(`/bookings/${id}`).then(r => r.data),

  cancel: (id: number) =>
    client.post<Booking>(`/bookings/${id}/cancel`).then(r => r.data),

  confirm: (id: number) =>
    client.post<Booking>(`/bookings/${id}/confirm`).then(r => r.data),

  checkIn: (id: number) =>
    client.post<Booking>(`/bookings/${id}/checkin`).then(r => r.data),

  checkOut: (id: number) =>
    client.post<Booking>(`/bookings/${id}/checkout`).then(r => r.data),
}

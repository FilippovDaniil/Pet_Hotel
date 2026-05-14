// API-клиент для billing-service: счета и оплата.
import client from './client'
import type { Invoice } from '../types'

export const billingApi = {
  // GET /api/invoices/my — счета текущего клиента (CUSTOMER).
  // Сервер фильтрует по X-User-Id заголовку от gateway.
  getMyInvoices: () =>
    client.get<Invoice[]>('/invoices/my').then(r => r.data),

  // GET /api/invoices/booking/{bookingId} — счёт по конкретному бронированию.
  // Используется в BookingDetailPage для отображения итогов.
  getByBooking: (bookingId: number) =>
    client.get<Invoice>(`/invoices/booking/${bookingId}`).then(r => r.data),

  // POST /api/invoices/{bookingId}/pay — оплатить счёт UNPAID→PAID (RECEPTION).
  // После оплаты billing-service публикует payment.processed в Kafka.
  pay: (bookingId: number) =>
    client.post<Invoice>(`/invoices/${bookingId}/pay`).then(r => r.data),
}

import client from './client'
import type { Invoice } from '../types'

export const billingApi = {
  getMyInvoices: () =>
    client.get<Invoice[]>('/invoices/my').then(r => r.data),

  getByBooking: (bookingId: number) =>
    client.get<Invoice>(`/invoices/booking/${bookingId}`).then(r => r.data),

  pay: (bookingId: number) =>
    client.post<Invoice>(`/invoices/${bookingId}/pay`).then(r => r.data),
}

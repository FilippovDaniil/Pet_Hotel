export type InvoiceStatus = 'UNPAID' | 'PAID'

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  UNPAID: 'Не оплачен',
  PAID:   'Оплачен',
}

export interface Invoice {
  id: number
  bookingId: number
  customerId: number
  roomAmount: number
  amenitiesAmount: number
  diningAmount: number
  totalAmount: number
  status: InvoiceStatus
  createdAt: string
}

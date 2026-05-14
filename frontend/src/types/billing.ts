// Типы счетов — соответствуют billing-service API.

export type InvoiceStatus = 'UNPAID' | 'PAID'

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  UNPAID: 'Не оплачен',
  PAID:   'Оплачен',
}

// Счёт из GET /api/invoices/my и GET /api/invoices/booking/{bookingId}.
// Три составляющие totalAmount: roomAmount + amenitiesAmount + diningAmount.
export interface Invoice {
  id: number
  bookingId: number
  customerId: number
  roomAmount: number       // стоимость проживания (ночи × цена номера)
  amenitiesAmount: number  // сумма дополнительных услуг (сауна, массаж и т.д.)
  diningAmount: number     // превышение лимита буфета (extraCharge из dining-service)
  totalAmount: number      // итог: roomAmount + amenitiesAmount + diningAmount
  status: InvoiceStatus
  createdAt: string        // ISO 8601 datetime
}

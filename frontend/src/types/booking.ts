export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'
export type ServiceType = 'SAUNA' | 'BATH' | 'POOL' | 'BILLIARD_RUS' | 'BILLIARD_US' | 'MASSAGE'

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING:   'Ожидает',
  CONFIRMED: 'Подтверждено',
  CANCELLED: 'Отменено',
  COMPLETED: 'Завершено',
}

export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  PENDING:   'bg-yellow-100 text-yellow-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
  COMPLETED: 'bg-blue-100 text-blue-700',
}

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  SAUNA:       'Сауна',
  BATH:        'Баня',
  POOL:        'Бассейн',
  BILLIARD_RUS:'Бильярд (Русский)',
  BILLIARD_US: 'Бильярд (Американка)',
  MASSAGE:     'Массаж',
}

export interface AmenityBooking {
  id: number
  serviceType: ServiceType
  startTime: string
  endTime: string
  price: number
}

export interface Booking {
  id: number
  customerId: number
  roomId: number
  roomClass: string
  checkInDate: string
  checkOutDate: string
  status: BookingStatus
  totalPrice: number
  createdAt: string
  amenities: AmenityBooking[]
}

export interface AmenityBookingRequest {
  serviceType: ServiceType
  startTime: string
  endTime: string
}

export interface BookingRequest {
  roomId: number
  checkIn: string
  checkOut: string
  amenities: AmenityBookingRequest[]
}

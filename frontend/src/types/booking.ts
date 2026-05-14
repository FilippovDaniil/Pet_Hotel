// Типы бронирований — соответствуют booking-service API.

// Жизненный цикл: PENDING → CONFIRMED → COMPLETED; PENDING/CONFIRMED → CANCELLED.
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'

// Типы дополнительных услуг — соответствуют common.enums.ServiceType.
export type ServiceType = 'SAUNA' | 'BATH' | 'POOL' | 'BILLIARD_RUS' | 'BILLIARD_US' | 'MASSAGE'

// Record<K, V>: объект с ключами типа K и значениями типа V.
// Здесь — человекочитаемые русские названия для отображения в UI.
export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING:   'Ожидает',
  CONFIRMED: 'Подтверждено',
  CANCELLED: 'Отменено',
  COMPLETED: 'Завершено',
}

// Tailwind CSS классы для цветового кодирования статуса (бейджи в карточках).
export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  PENDING:   'bg-yellow-100 text-yellow-700',  // жёлтый = ожидание
  CONFIRMED: 'bg-green-100 text-green-700',    // зелёный = подтверждено
  CANCELLED: 'bg-red-100 text-red-700',        // красный = отменено
  COMPLETED: 'bg-blue-100 text-blue-700',      // синий = завершено
}

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  SAUNA:       'Сауна',
  BATH:        'Баня',
  POOL:        'Бассейн',
  BILLIARD_RUS:'Бильярд (Русский)',
  BILLIARD_US: 'Бильярд (Американка)',
  MASSAGE:     'Массаж',
}

// Одна дополнительная услуга в составе бронирования (из ответа сервера).
export interface AmenityBooking {
  id: number
  serviceType: ServiceType
  startTime: string   // ISO 8601 datetime: "2025-08-01T10:00:00"
  endTime: string
  price: number       // итоговая цена с учётом привилегий класса номера
}

// Полное бронирование — ответ GET /api/bookings/{id} и GET /api/bookings/my.
export interface Booking {
  id: number
  customerId: number
  roomId: number
  roomClass: string       // ORDINARY | MIDDLE | PREMIUM
  checkInDate: string     // ISO date: "2025-08-01"
  checkOutDate: string
  status: BookingStatus
  totalPrice: number      // проживание + все услуги
  createdAt: string
  amenities: AmenityBooking[]
}

// Запрос на добавление услуги (без price — рассчитывается сервером).
export interface AmenityBookingRequest {
  serviceType: ServiceType
  startTime: string
  endTime: string
}

// Тело запроса POST /api/bookings — создать бронирование.
export interface BookingRequest {
  roomId: number
  checkIn: string           // ISO date: "2025-08-01"
  checkOut: string
  amenities: AmenityBookingRequest[]  // можно отправить пустой массив
}

// Типы номеров — соответствуют room-service API.

// Три класса номеров — влияют на привилегии буфета и доп. услуг.
export type RoomClass = 'ORDINARY' | 'MIDDLE' | 'PREMIUM'

// Русские названия для UI.
export const ROOM_CLASS_LABELS: Record<RoomClass, string> = {
  ORDINARY: 'Обычный',
  MIDDLE: 'Средний',
  PREMIUM: 'Премиум',
}

// Tailwind CSS классы для цветовых бейджей класса номера.
export const ROOM_CLASS_COLORS: Record<RoomClass, string> = {
  ORDINARY: 'bg-gray-100 text-gray-700',   // серый = стандарт
  MIDDLE: 'bg-blue-100 text-blue-700',     // синий = средний
  PREMIUM: 'bg-yellow-100 text-yellow-700', // золотой = премиум
}

// Ответ GET /api/rooms/{id} и GET /api/rooms.
export interface Room {
  id: number
  roomNumber: string  // "101", "203" — человекочитаемый номер
  roomClass: RoomClass
  capacity: number    // максимальное кол-во гостей
  pricePerNight: number
  description?: string
}

// Тело запроса POST /api/rooms и PUT /api/rooms/{id} (ADMIN).
export interface RoomRequest {
  roomNumber: string
  roomClass: RoomClass
  capacity: number
  pricePerNight: number
  description?: string
}

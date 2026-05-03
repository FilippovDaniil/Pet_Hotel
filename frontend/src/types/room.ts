export type RoomClass = 'ORDINARY' | 'MIDDLE' | 'PREMIUM'

export const ROOM_CLASS_LABELS: Record<RoomClass, string> = {
  ORDINARY: 'Обычный',
  MIDDLE: 'Средний',
  PREMIUM: 'Премиум',
}

export const ROOM_CLASS_COLORS: Record<RoomClass, string> = {
  ORDINARY: 'bg-gray-100 text-gray-700',
  MIDDLE: 'bg-blue-100 text-blue-700',
  PREMIUM: 'bg-yellow-100 text-yellow-700',
}

export interface Room {
  id: number
  roomNumber: string
  roomClass: RoomClass
  capacity: number
  pricePerNight: number
  description?: string
}

export interface RoomRequest {
  roomNumber: string
  roomClass: RoomClass
  capacity: number
  pricePerNight: number
  description?: string
}

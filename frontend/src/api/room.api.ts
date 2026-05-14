// API-клиент для room-service: просмотр и управление номерами.
import client from './client'
import type { Room, RoomRequest } from '../types'

export const roomApi = {
  // GET /api/rooms/available — поиск свободных номеров с фильтрами.
  // guests по умолчанию = 1, если не передан.
  // params → axios автоматически формирует query string: ?checkIn=...&checkOut=...&guests=...
  search: (checkIn: string, checkOut: string, guests: number = 1) =>
    client.get<Room[]>('/rooms/search', { params: { checkIn, checkOut, guests } }).then(r => r.data),

  // GET /api/rooms — все номера (без фильтрации).
  // Используется в ManageRoomsPage (ADMIN) для отображения полного списка.
  getAll: () =>
    client.get<Room[]>('/rooms').then(r => r.data),

  // GET /api/rooms/{id} — один номер по ID.
  getById: (id: number) =>
    client.get<Room>(`/rooms/${id}`).then(r => r.data),

  // POST /api/rooms — создать новый номер (ADMIN).
  create: (data: RoomRequest) =>
    client.post<Room>('/rooms', data).then(r => r.data),

  // PUT /api/rooms/{id} — обновить существующий номер (ADMIN).
  update: (id: number, data: RoomRequest) =>
    client.put<Room>(`/rooms/${id}`, data).then(r => r.data),

  // DELETE /api/rooms/{id} — удалить номер (ADMIN).
  // Не возвращает тело ответа (204 No Content) — поэтому нет .then(r => r.data).
  delete: (id: number) =>
    client.delete(`/rooms/${id}`),
}

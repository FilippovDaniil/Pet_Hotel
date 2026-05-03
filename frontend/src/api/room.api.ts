import client from './client'
import type { Room, RoomRequest } from '../types'

export const roomApi = {
  search: (checkIn: string, checkOut: string, guests: number = 1) =>
    client.get<Room[]>('/rooms/search', { params: { checkIn, checkOut, guests } }).then(r => r.data),

  getAll: () =>
    client.get<Room[]>('/rooms').then(r => r.data),

  getById: (id: number) =>
    client.get<Room>(`/rooms/${id}`).then(r => r.data),

  create: (data: RoomRequest) =>
    client.post<Room>('/rooms', data).then(r => r.data),

  update: (id: number, data: RoomRequest) =>
    client.put<Room>(`/rooms/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    client.delete(`/rooms/${id}`),
}

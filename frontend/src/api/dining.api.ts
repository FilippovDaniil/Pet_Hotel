import client from './client'
import type { MenuItem, MenuItemRequest, Order, OrderRequest } from '../types'

export const diningApi = {
  getMenu: () =>
    client.get<MenuItem[]>('/menu').then(r => r.data),

  createMenuItem: (data: MenuItemRequest) =>
    client.post<MenuItem>('/menu', data).then(r => r.data),

  updateMenuItem: (id: number, data: MenuItemRequest) =>
    client.put<MenuItem>(`/menu/${id}`, data).then(r => r.data),

  deleteMenuItem: (id: number) =>
    client.delete(`/menu/${id}`),

  createOrder: (data: OrderRequest) =>
    client.post<Order>('/orders', data).then(r => r.data),

  getOrdersByBooking: (bookingId: number) =>
    client.get<Order[]>(`/orders/booking/${bookingId}`).then(r => r.data),
}

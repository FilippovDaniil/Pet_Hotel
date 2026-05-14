// API-клиент для dining-service: меню и заказы буфета.
import client from './client'
import type { MenuItem, MenuItemRequest, Order, OrderRequest } from '../types'

export const diningApi = {
  // GET /api/menu — все позиции меню (публично).
  // Результат кэшируется в Redis на 1 час на стороне dining-service.
  getMenu: () =>
    client.get<MenuItem[]>('/menu').then(r => r.data),

  // POST /api/menu — добавить позицию меню (ADMIN).
  createMenuItem: (data: MenuItemRequest) =>
    client.post<MenuItem>('/menu', data).then(r => r.data),

  // PUT /api/menu/{id} — обновить позицию меню (ADMIN).
  // После обновления @CacheEvict(allEntries=true) сбрасывает Redis-кэш меню.
  updateMenuItem: (id: number, data: MenuItemRequest) =>
    client.put<MenuItem>(`/menu/${id}`, data).then(r => r.data),

  // DELETE /api/menu/{id} — удалить позицию меню (ADMIN).
  deleteMenuItem: (id: number) =>
    client.delete(`/menu/${id}`),

  // POST /api/orders — создать заказ буфета (CUSTOMER).
  // Сервер рассчитывает paidByLimit и extraCharge исходя из класса номера.
  // Если extraCharge > 0 → Kafka order.created → billing-service добавляет к счёту.
  createOrder: (data: OrderRequest) =>
    client.post<Order>('/orders', data).then(r => r.data),

  // GET /api/orders/booking/{bookingId} — все заказы по бронированию.
  // Используется в BookingDetailPage для отображения истории питания.
  getOrdersByBooking: (bookingId: number) =>
    client.get<Order[]>(`/orders/booking/${bookingId}`).then(r => r.data),

  // GET /api/orders/my — все заказы текущего клиента (фильтрует по X-User-Id).
  getMyOrders: () =>
    client.get<Order[]>('/orders/my').then(r => r.data),
}

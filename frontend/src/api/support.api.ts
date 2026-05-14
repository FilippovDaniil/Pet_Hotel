// API-клиент для support-service: чат между клиентом и администратором.
import client from './client'
import type { ConversationSummary, SupportMessage } from '../types'

export const supportApi = {
  // --- Методы для клиента (роль CUSTOMER) ---

  // GET /api/support/messages — вся переписка текущего клиента с admin.
  // Сервер фильтрует по X-User-Id заголовку от gateway.
  getMyMessages: () =>
    client.get<SupportMessage[]>('/support/messages').then((r) => r.data),

  // POST /api/support/messages — отправить сообщение в поддержку.
  // Тело: { content: string } — сервер добавляет customerId и senderRole=CUSTOMER.
  sendMessage: (content: string) =>
    client.post<SupportMessage>('/support/messages', { content }).then((r) => r.data),

  // GET /api/support/messages/unread-count — кол-во непрочитанных ответов от admin.
  // Используется для показа badge в Navbar (polling каждые 15 секунд).
  // Возвращает { count: number } — не просто число, чтобы JSON-парсинг работал корректно.
  getUnreadCount: () =>
    client.get<{ count: number }>('/support/messages/unread-count').then((r) => r.data),

  // POST /api/support/messages/read — пометить все ответы admin как прочитанные.
  // Вызывается при открытии чата клиентом — сбрасывает badge.
  markAsRead: () =>
    client.post('/support/messages/read').then((r) => r.data),

  // --- Методы для администратора (роль ADMIN) ---

  // GET /api/support/admin/conversations — список всех диалогов с клиентами.
  // Возвращает сводки (ConversationSummary), а не полную историю — для производительности.
  getConversations: () =>
    client.get<ConversationSummary[]>('/support/admin/conversations').then((r) => r.data),

  // GET /api/support/admin/conversations/{id} — полная история переписки с клиентом.
  // {id} — это customerId, не ID сообщения.
  getConversation: (customerId: number) =>
    client.get<SupportMessage[]>(`/support/admin/conversations/${customerId}`).then((r) => r.data),

  // POST /api/support/admin/conversations/{id}/messages — ответить клиенту (ADMIN).
  // Создаёт новое сообщение с senderRole=ADMIN в диалоге клиента.
  replyToCustomer: (customerId: number, content: string) =>
    client
      .post<SupportMessage>(`/support/admin/conversations/${customerId}/messages`, { content })
      .then((r) => r.data),

  // POST /api/support/admin/conversations/{id}/read — пометить сообщения клиента как прочитанные.
  // Сбрасывает счётчик unreadByAdmin в ConversationSummary.
  markConversationAsRead: (customerId: number) =>
    client.post(`/support/admin/conversations/${customerId}/read`).then((r) => r.data),
}

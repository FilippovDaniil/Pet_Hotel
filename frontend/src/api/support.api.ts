import client from './client'
import type { ConversationSummary, SupportMessage } from '../types'

export const supportApi = {
  // Customer
  getMyMessages: () =>
    client.get<SupportMessage[]>('/support/messages').then((r) => r.data),

  sendMessage: (content: string) =>
    client.post<SupportMessage>('/support/messages', { content }).then((r) => r.data),

  getUnreadCount: () =>
    client.get<{ count: number }>('/support/messages/unread-count').then((r) => r.data),

  markAsRead: () =>
    client.post('/support/messages/read').then((r) => r.data),

  // Admin
  getConversations: () =>
    client.get<ConversationSummary[]>('/support/admin/conversations').then((r) => r.data),

  getConversation: (customerId: number) =>
    client.get<SupportMessage[]>(`/support/admin/conversations/${customerId}`).then((r) => r.data),

  replyToCustomer: (customerId: number, content: string) =>
    client
      .post<SupportMessage>(`/support/admin/conversations/${customerId}/messages`, { content })
      .then((r) => r.data),

  markConversationAsRead: (customerId: number) =>
    client.post(`/support/admin/conversations/${customerId}/read`).then((r) => r.data),
}

// Типы чата поддержки — соответствуют support-service API.

// Кто отправил сообщение: клиент или администратор.
// Используется для визуального разделения пузырьков чата (слева/справа в UI).
export type SenderRole = 'CUSTOMER' | 'ADMIN'

// Одно сообщение в чате поддержки — ответ GET /api/support/messages.
// Одна таблица хранит переписку всех клиентов; фильтрация по customerId на стороне сервера.
export interface SupportMessage {
  id: number
  customerId: number       // ID клиента, которому принадлежит этот диалог
  customerEmail: string    // email клиента (денормализован для отображения в admin-панели)
  senderRole: SenderRole   // CUSTOMER = написал клиент, ADMIN = ответил администратор
  content: string          // текст сообщения
  createdAt: string        // ISO 8601 datetime
  readByCustomer: boolean  // true = клиент прочитал это сообщение от admin
  readByAdmin: boolean     // true = admin прочитал это сообщение от клиента
}

// Сводка по диалогу для admin-списка GET /api/support/admin/conversations.
// Не полная история, только последнее сообщение и счётчики непрочитанного.
export interface ConversationSummary {
  customerId: number
  customerEmail: string
  lastMessage: string      // текст последнего сообщения (для превью в списке)
  lastMessageAt: string    // ISO 8601 datetime последнего сообщения
  unreadByAdmin: number    // кол-во непрочитанных сообщений от клиента (badge для admin)
  unreadByCustomer: number // кол-во непрочитанных ответов от admin (badge в navbar клиента)
}

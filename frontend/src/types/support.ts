export type SenderRole = 'CUSTOMER' | 'ADMIN'

export interface SupportMessage {
  id: number
  customerId: number
  customerEmail: string
  senderRole: SenderRole
  content: string
  createdAt: string
  readByCustomer: boolean
  readByAdmin: boolean
}

export interface ConversationSummary {
  customerId: number
  customerEmail: string
  lastMessage: string
  lastMessageAt: string
  unreadByAdmin: number
  unreadByCustomer: number
}

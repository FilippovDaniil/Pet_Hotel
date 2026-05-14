// Типы буфета/столовой — соответствуют dining-service API.

// Два способа получить еду: доставка в номер или самостоятельно в столовой.
export type DeliveryType = 'ROOM_DELIVERY' | 'DINING_ROOM'

// Русские названия для выпадающего списка при создании заказа.
export const DELIVERY_TYPE_LABELS: Record<DeliveryType, string> = {
  ROOM_DELIVERY: 'Доставка в номер',
  DINING_ROOM: 'За столиком в столовой',
}

// Иконки-эмодзи для визуального различия типов в UI (не используются в backend).
export const DELIVERY_TYPE_ICONS: Record<DeliveryType, string> = {
  ROOM_DELIVERY: '🛏️',
  DINING_ROOM: '🍽️',
}

// Позиция меню — ответ GET /api/menu.
export interface MenuItem {
  id: number
  name: string
  price: number        // цена за единицу
  category: string     // "Завтраки", "Обед", "Напитки" и т.д. — произвольная строка
  available: boolean   // false = позиция временно недоступна (не отображается клиенту)
}

// Тело запроса POST /api/menu и PUT /api/menu/{id} (ADMIN).
export interface MenuItemRequest {
  name: string
  price: number
  category: string
  available: boolean
}

// Заказ еды — ответ GET /api/orders/booking/{bookingId}.
export interface Order {
  id: number
  bookingId: number
  customerId: number
  menuItemId: number
  menuItemName: string   // денормализовано: имя блюда на момент заказа (не меняется при редактировании меню)
  quantity: number
  totalAmount: number    // price × quantity
  orderTime: string      // ISO 8601 datetime
  paidByLimit: number    // часть, покрытая дневным лимитом класса номера
  extraCharge: number    // превышение лимита → уходит в billing-service через Kafka order.created
  deliveryType: DeliveryType
}

// Тело запроса POST /api/orders — создать заказ.
// Цена рассчитывается сервером; customerId берётся из X-User-Id заголовка (gateway).
export interface OrderRequest {
  bookingId: number
  menuItemId: number
  quantity: number
  deliveryType: DeliveryType
}

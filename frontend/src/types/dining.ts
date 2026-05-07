export type DeliveryType = 'ROOM_DELIVERY' | 'DINING_ROOM'

export const DELIVERY_TYPE_LABELS: Record<DeliveryType, string> = {
  ROOM_DELIVERY: 'Доставка в номер',
  DINING_ROOM: 'За столиком в столовой',
}

export const DELIVERY_TYPE_ICONS: Record<DeliveryType, string> = {
  ROOM_DELIVERY: '🛏️',
  DINING_ROOM: '🍽️',
}

export interface MenuItem {
  id: number
  name: string
  price: number
  category: string
  available: boolean
}

export interface MenuItemRequest {
  name: string
  price: number
  category: string
  available: boolean
}

export interface Order {
  id: number
  bookingId: number
  customerId: number
  menuItemId: number
  menuItemName: string
  quantity: number
  totalAmount: number
  orderTime: string
  paidByLimit: number
  extraCharge: number
  deliveryType: DeliveryType
}

export interface OrderRequest {
  bookingId: number
  menuItemId: number
  quantity: number
  deliveryType: DeliveryType
}

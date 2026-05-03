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
  quantity: number
  totalAmount: number
  orderTime: string
  paidByLimit: number
  extraCharge: number
}

export interface OrderRequest {
  bookingId: number
  menuItemId: number
  quantity: number
}

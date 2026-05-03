export type Role = 'CUSTOMER' | 'RECEPTION' | 'ADMIN'

export interface AuthResponse {
  token: string
  userId: number
  email: string
  role: Role
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface Customer {
  id: number
  email: string
  firstName: string
  lastName: string
  phone?: string
  role: Role
}

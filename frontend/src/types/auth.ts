// Типы аутентификации — соответствуют ответам customer-service API.

// Роли из common.enums.Role: три уровня доступа.
// Используется в RequireRole guard (App.tsx) и Navbar (показываем разные ссылки).
export type Role = 'CUSTOMER' | 'RECEPTION' | 'ADMIN'

// Ответ от POST /api/auth/login и POST /api/auth/register.
export interface AuthResponse {
  token: string   // JWT для всех последующих запросов (Authorization: Bearer <token>)
  userId: number
  email: string
  role: Role
}

// Тело запроса POST /api/auth/register.
export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string  // ? = необязательное поле
}

// Тело запроса POST /api/auth/login.
export interface LoginRequest {
  email: string
  password: string
}

// Данные клиента из GET /api/customers/{id} — используется в профиле.
export interface Customer {
  id: number
  email: string
  firstName: string
  lastName: string
  phone?: string
  role: Role
}

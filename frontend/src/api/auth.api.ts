// API-клиент для customer-service: аутентификация и управление пользователями.
import client from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, Customer } from '../types'

export const authApi = {
  // POST /api/auth/register — регистрация нового клиента.
  // Возвращает AuthResponse с JWT-токеном — сразу после регистрации пользователь залогинен.
  register: (data: RegisterRequest) =>
    client.post<AuthResponse>('/auth/register', data).then(r => r.data),

  // POST /api/auth/login — логин по email/password.
  login: (data: LoginRequest) =>
    client.post<AuthResponse>('/auth/login', data).then(r => r.data),

  // GET /api/customers/{id} — профиль текущего пользователя.
  // Используется в DashboardPage для отображения имени и роли.
  getMe: (userId: number) =>
    client.get<Customer>(`/customers/${userId}`).then(r => r.data),

  // GET /api/customers — все клиенты (ADMIN-доступ).
  getAllCustomers: () =>
    client.get<Customer[]>('/customers').then(r => r.data),

  // PUT /api/customers/{id}/role?role=RECEPTION — смена роли (ADMIN).
  // role передаётся как query-параметр (не в теле запроса).
  updateRole: (id: number, role: string) =>
    client.put<Customer>(`/customers/${id}/role?role=${role}`).then(r => r.data),
}

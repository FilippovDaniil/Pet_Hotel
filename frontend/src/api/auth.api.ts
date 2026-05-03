import client from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, Customer } from '../types'

export const authApi = {
  register: (data: RegisterRequest) =>
    client.post<AuthResponse>('/auth/register', data).then(r => r.data),

  login: (data: LoginRequest) =>
    client.post<AuthResponse>('/auth/login', data).then(r => r.data),

  getMe: (userId: number) =>
    client.get<Customer>(`/customers/${userId}`).then(r => r.data),

  getAllCustomers: () =>
    client.get<Customer[]>('/customers').then(r => r.data),

  updateRole: (id: number, role: string) =>
    client.put<Customer>(`/customers/${id}/role?role=${role}`).then(r => r.data),
}

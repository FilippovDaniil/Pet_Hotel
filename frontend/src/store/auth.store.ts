import { create } from 'zustand'
import type { AuthResponse, Role } from '../types'

interface AuthState {
  token: string | null
  userId: number | null
  email: string | null
  role: Role | null
  isAuthenticated: boolean
  login: (data: AuthResponse) => void
  logout: () => void
}

const loadFromStorage = (): Partial<AuthState> => {
  try {
    const token = localStorage.getItem('token')
    const userStr = localStorage.getItem('user')
    if (token && userStr) {
      const user = JSON.parse(userStr) as { userId: number; email: string; role: Role }
      return { token, userId: user.userId, email: user.email, role: user.role, isAuthenticated: true }
    }
  } catch {}
  return {}
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userId: null,
  email: null,
  role: null,
  isAuthenticated: false,
  ...loadFromStorage(),

  login: (data: AuthResponse) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify({ userId: data.userId, email: data.email, role: data.role }))
    set({
      token: data.token,
      userId: data.userId,
      email: data.email,
      role: data.role as Role,
      isAuthenticated: true,
    })
  },

  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ token: null, userId: null, email: null, role: null, isAuthenticated: false })
  },
}))

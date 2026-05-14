// Zustand store для управления аутентификацией.
// Zustand — легковесная альтернатива Redux: нет boilerplate, нет Provider.
// Компоненты подписываются через хук: const { role, isAuthenticated } = useAuthStore()
import { create } from 'zustand'
import type { AuthResponse, Role } from '../types'

// AuthState: тип state + методы мутации.
// Zustand объединяет данные и действия в одном объекте — в отличие от Redux (reducers отдельно).
interface AuthState {
  token: string | null
  userId: number | null
  email: string | null
  role: Role | null
  isAuthenticated: boolean  // флаг для RequireAuth guard в App.tsx
  login: (data: AuthResponse) => void
  logout: () => void
}

// loadFromStorage: восстанавливает session из localStorage при перезагрузке страницы.
// Без этого пользователь терял бы сессию при F5.
// try/catch: защита от невалидного JSON в localStorage (например, если данные повреждены).
const loadFromStorage = (): Partial<AuthState> => {
  try {
    const token = localStorage.getItem('token')
    const userStr = localStorage.getItem('user')
    if (token && userStr) {
      // as { userId: ... }: явное приведение типа после JSON.parse (который возвращает any)
      const user = JSON.parse(userStr) as { userId: number; email: string; role: Role }
      return { token, userId: user.userId, email: user.email, role: user.role, isAuthenticated: true }
    }
  } catch {}
  return {} // пустой объект — store инициализируется дефолтными значениями (null, false)
}

// create<AuthState>: создаёт хук useAuthStore с типизированным state.
// set — функция обновления state (иммутабельная замена, как setState в React).
export const useAuthStore = create<AuthState>((set) => ({
  // Начальные значения — переопределяются из localStorage через ...loadFromStorage().
  token: null,
  userId: null,
  email: null,
  role: null,
  isAuthenticated: false,
  ...loadFromStorage(), // spread: если в localStorage есть данные — перезаписывают дефолты

  // login: вызывается после успешного ответа от /api/auth/login или /api/auth/register.
  // Сохраняем в localStorage (для F5) И в Zustand state (для реактивности компонентов).
  login: (data: AuthResponse) => {
    localStorage.setItem('token', data.token)
    // Сохраняем только нужные поля (без лишних данных из ответа).
    localStorage.setItem('user', JSON.stringify({ userId: data.userId, email: data.email, role: data.role }))
    set({
      token: data.token,
      userId: data.userId,
      email: data.email,
      role: data.role as Role,
      isAuthenticated: true,
    })
  },

  // logout: очищает и localStorage, и Zustand state.
  // Компоненты, подписанные на isAuthenticated, автоматически перерендерятся.
  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ token: null, userId: null, email: null, role: null, isAuthenticated: false })
  },
}))

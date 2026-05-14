// Страница входа в систему — публичная, без RequireAuth.
import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../../api/auth.api'
import { useAuthStore } from '../../store/auth.store'

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)  // действие из Zustand: сохраняет токен и данные пользователя

  // Контролируемые поля формы: значения хранятся в state и синхронизируются с input.
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')         // строка ошибки; пустая = нет ошибки

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()      // отменяем стандартную перезагрузку страницы при submit
    setError('')             // сбрасываем предыдущую ошибку
    setLoading(true)
    try {
      const response = await authApi.login({ email, password })
      login(response)        // сохраняем токен + userId + role в Zustand и localStorage
      navigate('/dashboard') // переходим в личный кабинет
    } catch (err: any) {
      // Пробуем извлечь сообщение из ответа сервера (разные форматы ошибок).
      // err.response.data.message — стандартный формат Spring Boot ErrorResponse.
      // err.response.data — строковое сообщение без обёртки.
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Неверный email или пароль'
      setError(typeof msg === 'string' ? msg : 'Ошибка входа')
    } finally {
      setLoading(false)      // всегда снимаем loading после завершения запроса
    }
  }

  return (
    // min-h-screen: занимаем весь экран; gradient-to-br: диагональный градиент фона
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* .card — общий CSS-класс из index.css: белый блок с border-radius и padding */}
        <div className="card shadow-lg">
          {/* Логотип */}
          <div className="text-center mb-6">
            <div className="text-6xl mb-3">🏨</div>
            <h1 className="text-3xl font-bold text-gray-900">Pet Hotel</h1>
            <p className="text-gray-500 mt-1">Система управления отелем</p>
          </div>

          {/* Блок ошибки: показывается только если error непустая строка */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"               // браузер валидирует формат email
                className="input"          // .input из index.css
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"       // подсказка браузеру для автозаполнения
              />
            </div>

            <div>
              <label className="label" htmlFor="password">
                Пароль
              </label>
              <input
                id="password"
                type="password"            // скрывает символы
                className="input"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>

            <button
              type="submit"
              className="btn-primary w-full mt-2"
              disabled={loading}           // блокируем кнопку во время запроса
            >
              {loading ? (
                // Спиннер: CSS-анимация rotate через Tailwind animate-spin
                <span className="flex items-center gap-2">
                  <span className="animate-spin border-2 border-white border-t-transparent rounded-full w-4 h-4" />
                  Вход...
                </span>
              ) : (
                'Войти'
              )}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-4">
            Нет аккаунта?{' '}
            {/* Link — React Router компонент: SPA-навигация без перезагрузки */}
            <Link to="/register" className="text-primary-600 hover:underline font-medium">
              Зарегистрироваться
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

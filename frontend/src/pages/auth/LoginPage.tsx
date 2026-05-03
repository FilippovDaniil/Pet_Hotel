import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../../api/auth.api'
import { useAuthStore } from '../../store/auth.store'

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await authApi.login({ email, password })
      login(response)
      navigate('/dashboard')
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Неверный email или пароль'
      setError(typeof msg === 'string' ? msg : 'Ошибка входа')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="card shadow-lg">
          {/* Logo */}
          <div className="text-center mb-6">
            <div className="text-6xl mb-3">🏨</div>
            <h1 className="text-3xl font-bold text-gray-900">Pet Hotel</h1>
            <p className="text-gray-500 mt-1">Система управления отелем</p>
          </div>

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
                type="email"
                className="input"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            <div>
              <label className="label" htmlFor="password">
                Пароль
              </label>
              <input
                id="password"
                type="password"
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
              disabled={loading}
            >
              {loading ? (
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
            <Link to="/register" className="text-primary-600 hover:underline font-medium">
              Зарегистрироваться
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

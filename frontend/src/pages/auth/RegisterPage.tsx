import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../../api/auth.api'
import { useAuthStore } from '../../store/auth.store'

export default function RegisterPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (form.password.length < 6) {
      setError('Пароль должен содержать не менее 6 символов')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Пароли не совпадают')
      return
    }

    setLoading(true)
    try {
      const response = await authApi.register({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        phone: form.phone || undefined,
        password: form.password,
      })
      login(response)
      navigate('/dashboard')
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Ошибка регистрации'
      setError(typeof msg === 'string' ? msg : 'Ошибка регистрации')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-md">
        <div className="card shadow-lg">
          {/* Logo */}
          <div className="text-center mb-6">
            <div className="text-6xl mb-3">🏨</div>
            <h1 className="text-3xl font-bold text-gray-900">Pet Hotel</h1>
            <p className="text-gray-500 mt-1">Создание аккаунта</p>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label" htmlFor="firstName">
                  Имя
                </label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  className="input"
                  placeholder="Иван"
                  value={form.firstName}
                  onChange={handleChange}
                  required
                />
              </div>
              <div>
                <label className="label" htmlFor="lastName">
                  Фамилия
                </label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  className="input"
                  placeholder="Иванов"
                  value={form.lastName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div>
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                className="input"
                placeholder="you@example.com"
                value={form.email}
                onChange={handleChange}
                required
                autoComplete="email"
              />
            </div>

            <div>
              <label className="label" htmlFor="phone">
                Телефон{' '}
                <span className="text-gray-400 font-normal">(необязательно)</span>
              </label>
              <input
                id="phone"
                name="phone"
                type="tel"
                className="input"
                placeholder="+7 (999) 123-45-67"
                value={form.phone}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="label" htmlFor="password">
                Пароль{' '}
                <span className="text-gray-400 font-normal">(мин. 6 символов)</span>
              </label>
              <input
                id="password"
                name="password"
                type="password"
                className="input"
                placeholder="••••••••"
                value={form.password}
                onChange={handleChange}
                required
                autoComplete="new-password"
              />
            </div>

            <div>
              <label className="label" htmlFor="confirmPassword">
                Подтверждение пароля
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                className="input"
                placeholder="••••••••"
                value={form.confirmPassword}
                onChange={handleChange}
                required
                autoComplete="new-password"
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
                  Регистрация...
                </span>
              ) : (
                'Зарегистрироваться'
              )}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-4">
            Уже есть аккаунт?{' '}
            <Link to="/login" className="text-primary-600 hover:underline font-medium">
              Войти
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

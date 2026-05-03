import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth.store'
import { bookingApi } from '../api/booking.api'
import { billingApi } from '../api/billing.api'
import type { Booking, Invoice } from '../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

function StatCard({
  icon,
  title,
  value,
  color,
}: {
  icon: string
  title: string
  value: React.ReactNode
  color: string
}) {
  return (
    <div className="card flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl flex-shrink-0 ${color}`}>
        {icon}
      </div>
      <div>
        <p className="text-sm text-gray-500">{title}</p>
        <p className="text-2xl font-bold text-gray-900 mt-0.5">{value}</p>
      </div>
    </div>
  )
}

function QuickAction({
  icon,
  label,
  to,
  color,
}: {
  icon: string
  label: string
  to: string
  color: string
}) {
  const navigate = useNavigate()
  return (
    <button
      onClick={() => navigate(to)}
      className={`card flex flex-col items-center gap-2 p-5 cursor-pointer hover:shadow-md transition-shadow text-center group border-2 border-transparent hover:border-primary-200`}
    >
      <span className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl ${color} group-hover:scale-110 transition-transform`}>
        {icon}
      </span>
      <span className="text-sm font-medium text-gray-700">{label}</span>
    </button>
  )
}

// ── CUSTOMER dashboard ──────────────────────────────────────────────────────

function CustomerDashboard() {
  const { email } = useAuthStore()
  const [bookings, setBookings] = useState<Booking[]>([])
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      bookingApi.getMyBookings().catch(() => [] as Booking[]),
      billingApi.getMyInvoices().catch(() => [] as Invoice[]),
    ]).then(([b, inv]) => {
      setBookings(b)
      setInvoices(inv)
      setLoading(false)
    })
  }, [])

  const activeBookings = bookings.filter(
    (b) => b.status === 'PENDING' || b.status === 'CONFIRMED'
  )
  const nextCheckIn = activeBookings
    .map((b) => b.checkInDate)
    .sort()[0]
  const totalSpent = invoices
    .filter((i) => i.status === 'PAID')
    .reduce((sum, i) => sum + i.totalAmount, 0)

  const firstName = email?.split('@')[0] ?? 'Гость'

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Добро пожаловать, {firstName}! 👋</h1>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <StatCard
          icon="🛎️"
          title="Активные брони"
          value={activeBookings.length}
          color="bg-blue-50"
        />
        <StatCard
          icon="📅"
          title="Ближайший заезд"
          value={nextCheckIn ? new Date(nextCheckIn).toLocaleDateString('ru-RU') : '—'}
          color="bg-green-50"
        />
        <StatCard
          icon="💰"
          title="Потрачено всего"
          value={`${totalSpent.toLocaleString('ru-RU')} ₽`}
          color="bg-yellow-50"
        />
      </div>

      <h2 className="section-title">Быстрые действия</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <QuickAction icon="🔍" label="Найти номер" to="/rooms" color="bg-primary-50" />
        <QuickAction icon="📋" label="Мои брони" to="/bookings/my" color="bg-blue-50" />
        <QuickAction icon="🍽️" label="Меню буфета" to="/menu" color="bg-orange-50" />
        <QuickAction icon="🧾" label="Мои счета" to="/invoices" color="bg-green-50" />
      </div>
    </div>
  )
}

// ── RECEPTION dashboard ─────────────────────────────────────────────────────

function ReceptionDashboard() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    bookingApi
      .getAll()
      .catch(() => [] as Booking[])
      .then((b) => {
        setBookings(b)
        setLoading(false)
      })
  }, [])

  const pending = bookings.filter((b) => b.status === 'PENDING').length
  const confirmed = bookings.filter((b) => b.status === 'CONFIRMED').length

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Панель ресепшн 🛎️</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <StatCard
          icon="⏳"
          title="Ожидает подтверждения"
          value={pending}
          color="bg-yellow-50"
        />
        <StatCard
          icon="✅"
          title="Подтверждено"
          value={confirmed}
          color="bg-green-50"
        />
      </div>

      <h2 className="section-title">Быстрые действия</h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <QuickAction icon="📋" label="Все брони" to="/bookings/all" color="bg-primary-50" />
      </div>
    </div>
  )
}

// ── ADMIN dashboard ─────────────────────────────────────────────────────────

function AdminDashboard() {
  return (
    <div>
      <h1 className="page-title">Панель администратора ⚙️</h1>

      <h2 className="section-title">Быстрые действия</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <QuickAction icon="🏠" label="Управление номерами" to="/rooms/manage" color="bg-primary-50" />
        <QuickAction icon="🍽️" label="Управление меню" to="/menu/manage" color="bg-orange-50" />
        <QuickAction icon="🎯" label="Управление услугами" to="/amenities/manage" color="bg-purple-50" />
        <QuickAction icon="📋" label="Все брони" to="/bookings/all" color="bg-blue-50" />
      </div>
    </div>
  )
}

// ── Main export ─────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const role = useAuthStore((s) => s.role)

  if (role === 'CUSTOMER') return <CustomerDashboard />
  if (role === 'RECEPTION') return <ReceptionDashboard />
  if (role === 'ADMIN') return <AdminDashboard />

  return (
    <div className="flex items-center justify-center min-h-[40vh]">
      <p className="text-gray-500">Загрузка...</p>
    </div>
  )
}

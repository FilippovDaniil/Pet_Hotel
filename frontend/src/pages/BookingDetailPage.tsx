import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { bookingApi } from '../api/booking.api'
import { billingApi } from '../api/billing.api'
import { useAuthStore } from '../store/auth.store'
import type { Booking, Invoice } from '../types'
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_COLORS,
  SERVICE_TYPE_LABELS,
  INVOICE_STATUS_LABELS,
} from '../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

function formatDateTime(dt: string) {
  return new Date(dt).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function BookingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const bookingId = Number(id)
  const role = useAuthStore((s) => s.role)

  const [booking, setBooking] = useState<Booking | null>(null)
  const [invoice, setInvoice] = useState<Invoice | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [payLoading, setPayLoading] = useState(false)

  const fetchData = async () => {
    setLoading(true)
    try {
      const b = await bookingApi.getById(bookingId)
      setBooking(b)
      try {
        const inv = await billingApi.getByBooking(bookingId)
        setInvoice(inv)
      } catch {
        // invoice may not exist yet
      }
    } catch {
      setError('Не удалось загрузить данные брони')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (bookingId) fetchData()
  }, [bookingId])

  const doAction = async (action: () => Promise<Booking>) => {
    setActionError('')
    setActionLoading(true)
    try {
      const updated = await action()
      setBooking(updated)
      // Refresh invoice too
      try {
        const inv = await billingApi.getByBooking(bookingId)
        setInvoice(inv)
      } catch {}
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.response?.data || 'Ошибка операции'
      setActionError(typeof msg === 'string' ? msg : 'Ошибка операции')
    } finally {
      setActionLoading(false)
    }
  }

  const handlePay = async () => {
    if (!invoice) return
    setPayLoading(true)
    try {
      const updated = await billingApi.pay(bookingId)
      setInvoice(updated)
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.response?.data || 'Ошибка оплаты'
      setActionError(typeof msg === 'string' ? msg : 'Ошибка оплаты')
    } finally {
      setPayLoading(false)
    }
  }

  if (loading) return <Spinner />

  if (error || !booking) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
        {error || 'Бронь не найдена'}
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <Link to={role === 'CUSTOMER' ? '/bookings/my' : '/bookings/all'} className="btn-secondary text-sm">
          ← Назад
        </Link>
        <h1 className="page-title mb-0">Бронь #{booking.id}</h1>
      </div>

      {actionError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {actionError}
        </div>
      )}

      {/* Main booking card */}
      <div className="card mb-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="section-title mb-0">Детали брони</h2>
          <span
            className={`px-3 py-1 rounded-full text-sm font-semibold ${BOOKING_STATUS_COLORS[booking.status]}`}
          >
            {BOOKING_STATUS_LABELS[booking.status]}
          </span>
        </div>

        <dl className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
          <div>
            <dt className="text-gray-500">Клиент ID</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">{booking.customerId}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Номер ID</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">{booking.roomId}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Класс</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">{booking.roomClass || '—'}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Дата заезда</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">
              {new Date(booking.checkInDate).toLocaleDateString('ru-RU')}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Дата выезда</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">
              {new Date(booking.checkOutDate).toLocaleDateString('ru-RU')}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Создано</dt>
            <dd className="font-semibold text-gray-900 mt-0.5">
              {new Date(booking.createdAt).toLocaleDateString('ru-RU')}
            </dd>
          </div>
          <div className="col-span-2 sm:col-span-3">
            <dt className="text-gray-500">Итоговая стоимость</dt>
            <dd className="text-xl font-bold text-primary-700 mt-0.5">
              {booking.totalPrice.toLocaleString('ru-RU')} ₽
            </dd>
          </div>
        </dl>
      </div>

      {/* Amenities */}
      {booking.amenities?.length > 0 && (
        <div className="card mb-4">
          <h2 className="section-title">Дополнительные услуги</h2>
          <div className="overflow-x-auto">
            <table className="w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left font-semibold text-gray-600">Услуга</th>
                  <th className="px-4 py-2 text-left font-semibold text-gray-600">Начало</th>
                  <th className="px-4 py-2 text-left font-semibold text-gray-600">Конец</th>
                  <th className="px-4 py-2 text-right font-semibold text-gray-600">Стоимость</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {booking.amenities.map((a) => (
                  <tr key={a.id} className="hover:bg-gray-50">
                    <td className="px-4 py-2 font-medium text-gray-900">
                      {SERVICE_TYPE_LABELS[a.serviceType] ?? a.serviceType}
                    </td>
                    <td className="px-4 py-2 text-gray-600">
                      {formatDateTime(a.startTime)}
                    </td>
                    <td className="px-4 py-2 text-gray-600">
                      {formatDateTime(a.endTime)}
                    </td>
                    <td className="px-4 py-2 text-right font-semibold">
                      {a.price.toLocaleString('ru-RU')} ₽
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Action buttons */}
      <div className="card mb-4">
        <h2 className="section-title">Действия</h2>
        <div className="flex flex-wrap gap-3">
          {/* CUSTOMER actions */}
          {role === 'CUSTOMER' &&
            (booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
              <button
                className="btn-danger"
                onClick={() => doAction(() => bookingApi.cancel(bookingId))}
                disabled={actionLoading}
              >
                {actionLoading ? 'Обработка...' : 'Отменить бронь'}
              </button>
            )}

          {/* RECEPTION actions */}
          {role === 'RECEPTION' && booking.status === 'PENDING' && (
            <button
              className="btn-success"
              onClick={() => doAction(() => bookingApi.confirm(bookingId))}
              disabled={actionLoading}
            >
              {actionLoading ? 'Обработка...' : 'Подтвердить'}
            </button>
          )}
          {role === 'RECEPTION' && booking.status === 'CONFIRMED' && (
            <>
              <button
                className="btn-success"
                onClick={() => doAction(() => bookingApi.checkIn(bookingId))}
                disabled={actionLoading}
              >
                {actionLoading ? 'Обработка...' : 'Заселить'}
              </button>
              <button
                className="btn-secondary"
                onClick={() => doAction(() => bookingApi.checkOut(bookingId))}
                disabled={actionLoading}
              >
                {actionLoading ? 'Обработка...' : 'Выселить'}
              </button>
            </>
          )}

          {/* ADMIN same as RECEPTION */}
          {role === 'ADMIN' && booking.status === 'PENDING' && (
            <button
              className="btn-success"
              onClick={() => doAction(() => bookingApi.confirm(bookingId))}
              disabled={actionLoading}
            >
              {actionLoading ? 'Обработка...' : 'Подтвердить'}
            </button>
          )}
          {role === 'ADMIN' && booking.status === 'CONFIRMED' && (
            <>
              <button
                className="btn-success"
                onClick={() => doAction(() => bookingApi.checkIn(bookingId))}
                disabled={actionLoading}
              >
                {actionLoading ? 'Обработка...' : 'Заселить'}
              </button>
              <button
                className="btn-secondary"
                onClick={() => doAction(() => bookingApi.checkOut(bookingId))}
                disabled={actionLoading}
              >
                {actionLoading ? 'Обработка...' : 'Выселить'}
              </button>
            </>
          )}

          {(role === 'CUSTOMER' || role === 'ADMIN') &&
            booking.status !== 'PENDING' &&
            booking.status !== 'CONFIRMED' && (
              <p className="text-sm text-gray-500">Нет доступных действий</p>
            )}
        </div>
      </div>

      {/* Invoice */}
      {invoice && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="section-title mb-0">Счёт #{invoice.id}</h2>
            <span
              className={`px-3 py-1 rounded-full text-sm font-semibold ${
                invoice.status === 'PAID'
                  ? 'bg-green-100 text-green-700'
                  : 'bg-red-100 text-red-700'
              }`}
            >
              {INVOICE_STATUS_LABELS[invoice.status]}
            </span>
          </div>

          <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm mb-4">
            <div>
              <dt className="text-gray-500">Проживание</dt>
              <dd className="font-semibold text-gray-900 mt-0.5">
                {invoice.roomAmount.toLocaleString('ru-RU')} ₽
              </dd>
            </div>
            <div>
              <dt className="text-gray-500">Услуги</dt>
              <dd className="font-semibold text-gray-900 mt-0.5">
                {invoice.amenitiesAmount.toLocaleString('ru-RU')} ₽
              </dd>
            </div>
            <div>
              <dt className="text-gray-500">Питание</dt>
              <dd className="font-semibold text-gray-900 mt-0.5">
                {invoice.diningAmount.toLocaleString('ru-RU')} ₽
              </dd>
            </div>
            <div>
              <dt className="text-gray-500">Итого</dt>
              <dd className="text-lg font-bold text-primary-700 mt-0.5">
                {invoice.totalAmount.toLocaleString('ru-RU')} ₽
              </dd>
            </div>
          </dl>

          {role === 'RECEPTION' && invoice.status === 'UNPAID' && (
            <button
              className="btn-success"
              onClick={handlePay}
              disabled={payLoading}
            >
              {payLoading ? 'Обработка...' : 'Оплатить счёт'}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

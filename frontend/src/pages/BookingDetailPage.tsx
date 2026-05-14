// Страница деталей бронирования — доступна всем авторизованным ролям.
// Кнопки действий отображаются в зависимости от роли и текущего статуса.
import { useState, useEffect } from 'react'
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

// Форматирует ISO datetime в русский формат с датой и временем: "01.08.2025, 10:00".
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
  // useParams: извлекает :id из URL /bookings/:id
  const { id } = useParams<{ id: string }>()
  const bookingId = Number(id)
  const role = useAuthStore((s) => s.role)

  const [booking, setBooking] = useState<Booking | null>(null)
  const [invoice, setInvoice] = useState<Invoice | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')  // ошибка конкретного действия (confirm/cancel и т.д.)
  const [actionLoading, setActionLoading] = useState(false)
  const [payLoading, setPayLoading] = useState(false)

  const fetchData = async () => {
    setLoading(true)
    try {
      const b = await bookingApi.getById(bookingId)
      setBooking(b)
      try {
        // Счёт может ещё не существовать (создаётся только после booking.completed).
        const inv = await billingApi.getByBooking(bookingId)
        setInvoice(inv)
      } catch {
        // 404 — нормальная ситуация для незавершённых броней
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

  // Универсальная обёртка для действий (confirm, cancel, checkIn, checkOut).
  // action — лямбда, возвращающая Promise<Booking>.
  const doAction = async (action: () => Promise<Booking>) => {
    setActionError('')
    setActionLoading(true)
    try {
      const updated = await action()
      setBooking(updated)
      // После действия обновляем счёт (статус мог измениться)
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

  // Оплата счёта: отдельный loading-стейт, чтобы не блокировать другие кнопки.
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
        {/* Кнопка "Назад" ведёт на разные страницы в зависимости от роли */}
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

      {/* Основная информация о бронировании */}
      <div className="card mb-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="section-title mb-0">Детали брони</h2>
          <span
            className={`px-3 py-1 rounded-full text-sm font-semibold ${BOOKING_STATUS_COLORS[booking.status]}`}
          >
            {BOOKING_STATUS_LABELS[booking.status]}
          </span>
        </div>

        {/* dl/dt/dd — семантически корректно для пар "ключ: значение" */}
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
          {/* col-span-3: занимает всю строку сетки */}
          <div className="col-span-2 sm:col-span-3">
            <dt className="text-gray-500">Итоговая стоимость</dt>
            <dd className="text-xl font-bold text-primary-700 mt-0.5">
              {booking.totalPrice.toLocaleString('ru-RU')} ₽
            </dd>
          </div>
        </dl>
      </div>

      {/* Таблица дополнительных услуг */}
      {booking.amenities?.length > 0 && (
        <div className="card mb-4">
          <h2 className="section-title">Дополнительные услуги</h2>
          {/* overflow-x-auto: таблица скроллируется горизонтально на узких экранах */}
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
                      {/* ?? a.serviceType: если ключ неизвестен, показываем raw значение */}
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

      {/* Блок действий — кнопки зависят от роли и статуса */}
      <div className="card mb-4">
        <h2 className="section-title">Действия</h2>
        <div className="flex flex-wrap gap-3">
          {/* Клиент: только отмена активных броней */}
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

          {/* Ресепшн: подтверждение PENDING, заселение/выселение CONFIRMED */}
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

          {/* Администратор: те же действия что и ресепшн */}
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

          {/* Нет доступных действий для завершённых/отменённых броней */}
          {(role === 'CUSTOMER' || role === 'ADMIN') &&
            booking.status !== 'PENDING' &&
            booking.status !== 'CONFIRMED' && (
              <p className="text-sm text-gray-500">Нет доступных действий</p>
            )}
        </div>
      </div>

      {/* Счёт — отображается только если уже создан (booking.completed) */}
      {invoice && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="section-title mb-0">Счёт #{invoice.id}</h2>
            <span
              className={`px-3 py-1 rounded-full text-sm font-semibold ${
                invoice.status === 'PAID'
                  ? 'bg-green-100 text-green-700'  // зелёный = оплачен
                  : 'bg-red-100 text-red-700'      // красный = не оплачен
              }`}
            >
              {INVOICE_STATUS_LABELS[invoice.status]}
            </span>
          </div>

          {/* Три составляющие счёта: проживание + услуги + питание */}
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

          {/* Кнопка оплаты доступна только ресепшну для неоплаченных счетов */}
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

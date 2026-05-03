import React, { useState, useEffect } from 'react'
import { diningApi } from '../../api/dining.api'
import { bookingApi } from '../../api/booking.api'
import type { MenuItem, Booking } from '../../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

interface OrderModalProps {
  item: MenuItem
  bookings: Booking[]
  onClose: () => void
  onSuccess: () => void
}

function OrderModal({ item, bookings, onClose, onSuccess }: OrderModalProps) {
  const [bookingId, setBookingId] = useState<number | ''>(
    bookings.length > 0 ? bookings[0].id : ''
  )
  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!bookingId) {
      setError('Выберите бронь')
      return
    }
    setLoading(true)
    setError('')
    try {
      await diningApi.createOrder({
        bookingId: Number(bookingId),
        menuItemId: item.id,
        quantity,
      })
      onSuccess()
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.response?.data || 'Ошибка заказа'
      setError(typeof msg === 'string' ? msg : 'Ошибка заказа')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold text-gray-900 mb-1">{item.name}</h3>
        <p className="text-sm text-gray-500 mb-5">
          {item.price.toLocaleString('ru-RU')} ₽ за единицу
        </p>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        {bookings.length === 0 ? (
          <p className="text-gray-500 text-sm mb-4">
            У вас нет активных броней для прикрепления заказа.
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label" htmlFor="order-booking">
                Бронь
              </label>
              <select
                id="order-booking"
                className="input"
                value={bookingId}
                onChange={(e) => setBookingId(Number(e.target.value))}
                required
              >
                {bookings.map((b) => (
                  <option key={b.id} value={b.id}>
                    Бронь #{b.id} — Номер #{b.roomId} (
                    {new Date(b.checkInDate).toLocaleDateString('ru-RU')} –{' '}
                    {new Date(b.checkOutDate).toLocaleDateString('ru-RU')})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="label" htmlFor="order-qty">
                Количество
              </label>
              <input
                id="order-qty"
                type="number"
                className="input"
                min={1}
                max={20}
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
                required
              />
            </div>

            <div className="pt-1 text-sm text-gray-600">
              Итого:{' '}
              <strong className="text-gray-900">
                {(item.price * quantity).toLocaleString('ru-RU')} ₽
              </strong>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                className="btn-secondary flex-1"
                onClick={onClose}
              >
                Отмена
              </button>
              <button
                type="submit"
                className="btn-primary flex-1"
                disabled={loading}
              >
                {loading ? 'Заказ...' : 'Заказать'}
              </button>
            </div>
          </form>
        )}

        {bookings.length === 0 && (
          <button className="btn-secondary w-full" onClick={onClose}>
            Закрыть
          </button>
        )}
      </div>
    </div>
  )
}

export default function MenuPage() {
  const [menuItems, setMenuItems] = useState<MenuItem[]>([])
  const [activeBookings, setActiveBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [orderItem, setOrderItem] = useState<MenuItem | null>(null)
  const [successMsg, setSuccessMsg] = useState('')

  useEffect(() => {
    Promise.all([
      diningApi.getMenu().catch(() => [] as MenuItem[]),
      bookingApi
        .getMyBookings()
        .then((bs) =>
          bs.filter((b) => b.status === 'PENDING' || b.status === 'CONFIRMED')
        )
        .catch(() => [] as Booking[]),
    ])
      .then(([menu, bookings]) => {
        setMenuItems(menu)
        setActiveBookings(bookings)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить меню')
        setLoading(false)
      })
  }, [])

  // Group by category
  const categories = Array.from(
    new Set(menuItems.filter((m) => m.available).map((m) => m.category))
  )

  const handleOrderSuccess = () => {
    setOrderItem(null)
    setSuccessMsg('Заказ успешно оформлен!')
    setTimeout(() => setSuccessMsg(''), 4000)
  }

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Меню буфета</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {successMsg && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {successMsg}
        </div>
      )}

      {categories.length === 0 && !error && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">🍽️</p>
          <p className="text-lg font-medium">Меню пока не добавлено</p>
        </div>
      )}

      {categories.map((category) => (
        <div key={category} className="mb-8">
          <h2 className="section-title capitalize">{category}</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {menuItems
              .filter((m) => m.category === category && m.available)
              .map((item) => (
                <div
                  key={item.id}
                  className="card hover:shadow-md transition-shadow flex flex-col gap-3"
                >
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold text-gray-900">{item.name}</h3>
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full whitespace-nowrap">
                      {item.category}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mt-auto">
                    <span className="text-lg font-bold text-primary-700">
                      {item.price.toLocaleString('ru-RU')} ₽
                    </span>
                    <button
                      className="btn-primary text-sm"
                      onClick={() => setOrderItem(item)}
                    >
                      Заказать
                    </button>
                  </div>
                </div>
              ))}
          </div>
        </div>
      ))}

      {orderItem && (
        <OrderModal
          item={orderItem}
          bookings={activeBookings}
          onClose={() => setOrderItem(null)}
          onSuccess={handleOrderSuccess}
        />
      )}
    </div>
  )
}

import { useState, useEffect } from 'react'
import { diningApi } from '../../api/dining.api'
import type { Order } from '../../types'
import { DELIVERY_TYPE_LABELS, DELIVERY_TYPE_ICONS } from '../../types'
import { Link } from 'react-router-dom'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function MyOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    diningApi
      .getMyOrders()
      .then((data) => {
        setOrders(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить заказы')
        setLoading(false)
      })
  }, [])

  const totalSpent = orders.reduce((s, o) => s + Number(o.totalAmount), 0)

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Мои заказы</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {orders.length === 0 && !error && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">🍽️</p>
          <p className="text-lg font-medium mb-2">Заказов пока нет</p>
          <Link to="/menu" className="btn-primary">
            Перейти в меню
          </Link>
        </div>
      )}

      {orders.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div className="card flex items-center gap-4">
              <div className="w-10 h-10 bg-orange-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
                🧾
              </div>
              <div>
                <p className="text-sm text-gray-500">Всего заказов</p>
                <p className="text-2xl font-bold text-gray-900">{orders.length}</p>
              </div>
            </div>
            <div className="card flex items-center gap-4">
              <div className="w-10 h-10 bg-green-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
                💰
              </div>
              <div>
                <p className="text-sm text-gray-500">Итого потрачено</p>
                <p className="text-2xl font-bold text-gray-900">
                  {totalSpent.toLocaleString('ru-RU')} ₽
                </p>
              </div>
            </div>
            <div className="card flex items-center gap-4">
              <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
                🍴
              </div>
              <div>
                <p className="text-sm text-gray-500">Последний заказ</p>
                <p className="text-sm font-semibold text-gray-900">
                  {orders[0] ? formatDate(orders[0].orderTime) : '—'}
                </p>
              </div>
            </div>
          </div>

          <div className="space-y-3">
            {orders.map((order) => (
              <div key={order.id} className="card hover:shadow-md transition-shadow">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-2 flex-wrap">
                      <span className="font-bold text-gray-900 text-base">
                        {order.menuItemName || `Позиция #${order.menuItemId}`}
                      </span>
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          order.deliveryType === 'ROOM_DELIVERY'
                            ? 'bg-blue-100 text-blue-700'
                            : 'bg-orange-100 text-orange-700'
                        }`}
                      >
                        {DELIVERY_TYPE_ICONS[order.deliveryType]}{' '}
                        {DELIVERY_TYPE_LABELS[order.deliveryType]}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-6 gap-y-1 text-sm">
                      <div>
                        <span className="text-gray-400">Количество:</span>{' '}
                        <strong className="text-gray-700">{order.quantity} шт.</strong>
                      </div>
                      <div>
                        <span className="text-gray-400">Сумма:</span>{' '}
                        <strong className="text-gray-900">
                          {Number(order.totalAmount).toLocaleString('ru-RU')} ₽
                        </strong>
                      </div>
                      {Number(order.paidByLimit) > 0 && (
                        <div>
                          <span className="text-gray-400">По лимиту:</span>{' '}
                          <strong className="text-green-700">
                            {Number(order.paidByLimit).toLocaleString('ru-RU')} ₽
                          </strong>
                        </div>
                      )}
                      {Number(order.extraCharge) > 0 && (
                        <div>
                          <span className="text-gray-400">Доп. оплата:</span>{' '}
                          <strong className="text-red-600">
                            {Number(order.extraCharge).toLocaleString('ru-RU')} ₽
                          </strong>
                        </div>
                      )}
                    </div>

                    <p className="text-xs text-gray-400 mt-2">
                      {formatDate(order.orderTime)} · Бронь #{order.bookingId}
                    </p>
                  </div>

                  <Link
                    to={`/bookings/${order.bookingId}`}
                    className="btn-secondary text-xs py-1 px-3 flex-shrink-0"
                  >
                    Бронь →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

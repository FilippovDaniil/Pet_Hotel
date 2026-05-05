import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { bookingApi } from '../../api/booking.api'
import type { Booking } from '../../types'
import { BOOKING_STATUS_LABELS, BOOKING_STATUS_COLORS } from '../../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [confirmCancelId, setConfirmCancelId] = useState<number | null>(null)

  const fetchBookings = () => {
    setLoading(true)
    bookingApi
      .getMyBookings()
      .then((data) => {
        setBookings(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить брони')
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchBookings()
  }, [])

  const handleCancel = async (id: number) => {
    setCancellingId(id)
    setConfirmCancelId(null)
    try {
      await bookingApi.cancel(id)
      fetchBookings()
    } catch {
      setError('Не удалось отменить бронь')
    } finally {
      setCancellingId(null)
    }
  }

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Мои брони</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {bookings.length === 0 && !error && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">📋</p>
          <p className="text-lg font-medium">У вас пока нет броней</p>
          <p className="text-sm mt-1">
            <Link to="/rooms" className="text-primary-600 hover:underline">
              Найдите номер
            </Link>{' '}
            и создайте бронь
          </p>
        </div>
      )}

      <div className="space-y-4">
        {bookings.map((booking) => (
          <div key={booking.id} className="card hover:shadow-md transition-shadow">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <h3 className="font-bold text-gray-900">
                    Бронь #{booking.id}
                  </h3>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${BOOKING_STATUS_COLORS[booking.status]}`}
                  >
                    {BOOKING_STATUS_LABELS[booking.status]}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-6 gap-y-1 text-sm text-gray-600">
                  <div>
                    <span className="text-gray-400">Номер:</span>{' '}
                    <strong className="text-gray-900">#{booking.roomId}</strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Заезд:</span>{' '}
                    <strong className="text-gray-900">
                      {new Date(booking.checkInDate).toLocaleDateString('ru-RU')}
                    </strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Выезд:</span>{' '}
                    <strong className="text-gray-900">
                      {new Date(booking.checkOutDate).toLocaleDateString('ru-RU')}
                    </strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Итого:</span>{' '}
                    <strong className="text-gray-900">
                      {booking.totalPrice.toLocaleString('ru-RU')} ₽
                    </strong>
                  </div>
                </div>

                {booking.amenities?.length > 0 && (
                  <p className="text-xs text-gray-400 mt-1">
                    Услуги: {booking.amenities.length}
                  </p>
                )}
              </div>

              <div className="flex gap-2 flex-shrink-0">
                <Link
                  to={`/bookings/${booking.id}`}
                  className="btn-secondary text-sm"
                >
                  Подробнее
                </Link>

                {(booking.status === 'PENDING' ||
                  booking.status === 'CONFIRMED') && (
                  <button
                    className="btn-danger text-sm"
                    onClick={() => setConfirmCancelId(booking.id)}
                    disabled={cancellingId === booking.id}
                  >
                    {cancellingId === booking.id ? 'Отмена...' : 'Отменить'}
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Confirm cancel dialog */}
      {confirmCancelId !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-bold text-gray-900 mb-2">
              Отменить бронь?
            </h3>
            <p className="text-gray-500 text-sm mb-5">
              Вы уверены, что хотите отменить бронь #{confirmCancelId}? Это
              действие необратимо.
            </p>
            <div className="flex gap-3">
              <button
                className="btn-secondary flex-1"
                onClick={() => setConfirmCancelId(null)}
              >
                Нет, оставить
              </button>
              <button
                className="btn-danger flex-1"
                onClick={() => handleCancel(confirmCancelId)}
              >
                Да, отменить
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

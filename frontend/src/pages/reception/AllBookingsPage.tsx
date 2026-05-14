// Страница всех бронирований системы (RECEPTION, ADMIN).
// Включает фильтрацию по статусу (табы) и текстовый поиск.
import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { bookingApi } from '../../api/booking.api'
import type { Booking, BookingStatus } from '../../types'
import { BOOKING_STATUS_LABELS, BOOKING_STATUS_COLORS } from '../../types'

// Конфигурация табов фильтрации: ALL + каждый статус.
const STATUS_TABS: { label: string; value: BookingStatus | 'ALL' }[] = [
  { label: 'Все', value: 'ALL' },
  { label: 'Ожидает', value: 'PENDING' },
  { label: 'Подтверждено', value: 'CONFIRMED' },
  { label: 'Завершено', value: 'COMPLETED' },
  { label: 'Отменено', value: 'CANCELLED' },
]

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function AllBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<BookingStatus | 'ALL'>('ALL')
  const [search, setSearch] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)  // ID строки с активной операцией

  const fetchBookings = () => {
    setLoading(true)
    bookingApi
      .getAll()
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

  // Выполнить действие над конкретной бронью и обновить её в списке (без полного рефетча).
  const doAction = async (
    id: number,
    action: () => Promise<Booking>
  ) => {
    setActionLoadingId(id)
    try {
      const updated = await action()
      // Оптимистичное обновление: заменяем только изменённую запись
      setBookings((prev) =>
        prev.map((b) => (b.id === updated.id ? updated : b))
      )
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          'Ошибка операции'
      )
    } finally {
      setActionLoadingId(null)
    }
  }

  // Клиентская фильтрация: таб + текстовый поиск по ID брони/клиента/номера.
  const filtered = bookings.filter((b) => {
    const matchesTab = activeTab === 'ALL' || b.status === activeTab
    const q = search.toLowerCase()
    const matchesSearch =
      !q ||
      String(b.id).includes(q) ||
      String(b.customerId).includes(q) ||
      String(b.roomId).includes(q)
    return matchesTab && matchesSearch
  })

  return (
    <div>
      <h1 className="page-title">Все брони</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {/* Поле поиска */}
      <div className="card mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center">
        <input
          type="text"
          className="input max-w-xs"
          placeholder="Поиск по ID брони / клиента / номера..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {/* Табы фильтрации по статусу с счётчиками */}
      <div className="flex gap-1 mb-4 flex-wrap">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => setActiveTab(tab.value)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === tab.value
                ? 'bg-primary-600 text-white'
                : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {tab.label}
            {/* Счётчик рядом с названием таба */}
            <span className="ml-1.5 text-xs opacity-70">
              {tab.value === 'ALL'
                ? bookings.length
                : bookings.filter((b) => b.status === tab.value).length}
            </span>
          </button>
        ))}
      </div>

      {loading && <Spinner />}

      {!loading && filtered.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-4xl mb-3">📋</p>
          <p>Нет броней по заданным фильтрам</p>
        </div>
      )}

      {/* Таблица бронирований */}
      {!loading && filtered.length > 0 && (
        // p-0: убираем padding карточки, чтобы таблица прилегала к краям
        <div className="card overflow-x-auto p-0">
          <table className="w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {/* Заголовки колонок генерируются из массива — избегаем дублирования */}
                {[
                  'ID',
                  'Клиент',
                  'Номер',
                  'Заезд',
                  'Выезд',
                  'Статус',
                  'Сумма',
                  'Действия',
                ].map((h) => (
                  <th
                    key={h}
                    className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {filtered.map((booking) => (
                <tr key={booking.id} className="hover:bg-gray-50">
                  {/* font-mono: моноширинный шрифт для числовых ID */}
                  <td className="px-4 py-3 font-mono font-semibold text-gray-900">
                    #{booking.id}
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    #{booking.customerId}
                  </td>
                  <td className="px-4 py-3 text-gray-600">#{booking.roomId}</td>
                  <td className="px-4 py-3 whitespace-nowrap text-gray-600">
                    {new Date(booking.checkInDate).toLocaleDateString('ru-RU')}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-gray-600">
                    {new Date(booking.checkOutDate).toLocaleDateString('ru-RU')}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap ${BOOKING_STATUS_COLORS[booking.status]}`}
                    >
                      {BOOKING_STATUS_LABELS[booking.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-semibold text-gray-900 whitespace-nowrap">
                    {booking.totalPrice.toLocaleString('ru-RU')} ₽
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1 flex-wrap">
                      <Link
                        to={`/bookings/${booking.id}`}
                        className="btn-secondary text-xs py-1 px-2"
                      >
                        Подробнее
                      </Link>

                      {/* Подтвердить: только PENDING */}
                      {booking.status === 'PENDING' && (
                        <button
                          className="btn-success text-xs py-1 px-2"
                          disabled={actionLoadingId === booking.id}
                          onClick={() =>
                            doAction(booking.id, () =>
                              bookingApi.confirm(booking.id)
                            )
                          }
                        >
                          {actionLoadingId === booking.id
                            ? '...'
                            : 'Подтвердить'}
                        </button>
                      )}

                      {/* Заселить/Выселить: только CONFIRMED */}
                      {booking.status === 'CONFIRMED' && (
                        <>
                          <button
                            className="btn-success text-xs py-1 px-2"
                            disabled={actionLoadingId === booking.id}
                            onClick={() =>
                              doAction(booking.id, () =>
                                bookingApi.checkIn(booking.id)
                              )
                            }
                          >
                            {actionLoadingId === booking.id ? '...' : 'Заселить'}
                          </button>
                          <button
                            className="btn-secondary text-xs py-1 px-2"
                            disabled={actionLoadingId === booking.id}
                            onClick={() =>
                              doAction(booking.id, () =>
                                bookingApi.checkOut(booking.id)
                              )
                            }
                          >
                            {actionLoadingId === booking.id
                              ? '...'
                              : 'Выселить'}
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

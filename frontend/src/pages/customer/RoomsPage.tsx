import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { roomApi } from '../../api/room.api'
import type { Room } from '../../types'
import { ROOM_CLASS_LABELS, ROOM_CLASS_COLORS } from '../../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function RoomsPage() {
  const navigate = useNavigate()

  const today = new Date().toISOString().split('T')[0]
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0]

  const [checkIn, setCheckIn] = useState(today)
  const [checkOut, setCheckOut] = useState(tomorrow)
  const [guests, setGuests] = useState(1)
  const [rooms, setRooms] = useState<Room[]>([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const results = await roomApi.search(checkIn, checkOut, guests)
      setRooms(results)
      setSearched(true)
    } catch (err: any) {
      setError('Ошибка при поиске номеров')
    } finally {
      setLoading(false)
    }
  }

  const nights =
    checkIn && checkOut
      ? Math.max(
          1,
          Math.round(
            (new Date(checkOut).getTime() - new Date(checkIn).getTime()) /
              86400000
          )
        )
      : 1

  const handleBook = (room: Room) => {
    navigate(
      `/bookings/new?roomId=${room.id}&checkIn=${checkIn}&checkOut=${checkOut}`
    )
  }

  return (
    <div>
      <h1 className="page-title">Поиск номеров</h1>

      {/* Search form */}
      <div className="card mb-6">
        <form
          onSubmit={handleSearch}
          className="flex flex-wrap gap-4 items-end"
        >
          <div className="flex-1 min-w-[140px]">
            <label className="label" htmlFor="checkIn">
              Дата заезда
            </label>
            <input
              id="checkIn"
              type="date"
              className="input"
              value={checkIn}
              min={today}
              onChange={(e) => setCheckIn(e.target.value)}
              required
            />
          </div>
          <div className="flex-1 min-w-[140px]">
            <label className="label" htmlFor="checkOut">
              Дата выезда
            </label>
            <input
              id="checkOut"
              type="date"
              className="input"
              value={checkOut}
              min={checkIn || today}
              onChange={(e) => setCheckOut(e.target.value)}
              required
            />
          </div>
          <div className="w-32">
            <label className="label" htmlFor="guests">
              Гостей
            </label>
            <input
              id="guests"
              type="number"
              className="input"
              value={guests}
              min={1}
              max={10}
              onChange={(e) => setGuests(Number(e.target.value))}
              required
            />
          </div>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Поиск...' : '🔍 Найти'}
          </button>
        </form>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {loading && <Spinner />}

      {!loading && !searched && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">🏨</p>
          <p className="text-lg font-medium">Введите даты и нажмите «Найти»</p>
          <p className="text-sm mt-1">Мы покажем доступные номера</p>
        </div>
      )}

      {!loading && searched && rooms.length === 0 && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">😔</p>
          <p className="text-lg font-medium">Номера не найдены</p>
          <p className="text-sm mt-1">Попробуйте изменить даты или количество гостей</p>
        </div>
      )}

      {!loading && rooms.length > 0 && (
        <>
          <p className="text-sm text-gray-500 mb-4">
            Найдено номеров: <strong>{rooms.length}</strong> · {nights}{' '}
            {nights === 1 ? 'ночь' : nights < 5 ? 'ночи' : 'ночей'}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {rooms.map((room) => (
              <div key={room.id} className="card hover:shadow-md transition-shadow flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-bold text-gray-900">
                    Номер #{room.roomNumber}
                  </h3>
                  <span
                    className={`px-2.5 py-1 rounded-full text-xs font-semibold ${ROOM_CLASS_COLORS[room.roomClass]}`}
                  >
                    {ROOM_CLASS_LABELS[room.roomClass]}
                  </span>
                </div>

                <div className="flex items-center gap-4 text-sm text-gray-600">
                  <span>👥 {room.capacity} гост.</span>
                  <span>💰 {room.pricePerNight.toLocaleString('ru-RU')} ₽/ночь</span>
                </div>

                {room.description && (
                  <p className="text-sm text-gray-500 line-clamp-2">
                    {room.description}
                  </p>
                )}

                <div className="pt-2 border-t border-gray-100 flex items-center justify-between mt-auto">
                  <span className="text-sm text-gray-500">
                    Итого:{' '}
                    <strong className="text-gray-900">
                      {(room.pricePerNight * nights).toLocaleString('ru-RU')} ₽
                    </strong>
                  </span>
                  <button
                    className="btn-primary"
                    onClick={() => handleBook(room)}
                  >
                    Забронировать
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

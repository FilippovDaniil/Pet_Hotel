import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { roomApi } from '../../api/room.api'
import { bookingApi } from '../../api/booking.api'
import type { Room, ServiceType, AmenityBookingRequest } from '../../types'
import { SERVICE_TYPE_LABELS, ROOM_CLASS_LABELS, ROOM_CLASS_COLORS } from '../../types'

const ALL_SERVICES: ServiceType[] = [
  'SAUNA',
  'BATH',
  'POOL',
  'BILLIARD_RUS',
  'BILLIARD_US',
  'MASSAGE',
]

interface AmenityEntry {
  checked: boolean
  startTime: string
  endTime: string
}

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function BookingCreatePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const roomId = Number(searchParams.get('roomId'))
  const checkIn = searchParams.get('checkIn') ?? ''
  const checkOut = searchParams.get('checkOut') ?? ''

  const [room, setRoom] = useState<Room | null>(null)
  const [loadingRoom, setLoadingRoom] = useState(true)
  const [roomError, setRoomError] = useState('')

  const defaultEntry = (): AmenityEntry => ({
    checked: false,
    startTime: '',
    endTime: '',
  })
  const [amenities, setAmenities] = useState<Record<ServiceType, AmenityEntry>>(
    () =>
      Object.fromEntries(
        ALL_SERVICES.map((s) => [s, defaultEntry()])
      ) as Record<ServiceType, AmenityEntry>
  )

  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  useEffect(() => {
    if (!roomId) {
      setRoomError('Не указан номер комнаты')
      setLoadingRoom(false)
      return
    }
    roomApi
      .getById(roomId)
      .then((r) => {
        setRoom(r)
        setLoadingRoom(false)
      })
      .catch(() => {
        setRoomError('Не удалось загрузить информацию о номере')
        setLoadingRoom(false)
      })
  }, [roomId])

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

  const roomCost = (room?.pricePerNight ?? 0) * nights

  const toggleAmenity = (type: ServiceType) => {
    setAmenities((prev) => ({
      ...prev,
      [type]: { ...prev[type], checked: !prev[type].checked },
    }))
  }

  const updateAmenity = (
    type: ServiceType,
    field: 'startTime' | 'endTime',
    value: string
  ) => {
    setAmenities((prev) => ({
      ...prev,
      [type]: { ...prev[type], [field]: value },
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitError('')

    const selectedAmenities: AmenityBookingRequest[] = ALL_SERVICES.filter(
      (s) => amenities[s].checked
    ).map((s) => ({
      serviceType: s,
      startTime: amenities[s].startTime,
      endTime: amenities[s].endTime,
    }))

    for (const a of selectedAmenities) {
      if (!a.startTime || !a.endTime) {
        setSubmitError(
          `Укажите время для услуги «${SERVICE_TYPE_LABELS[a.serviceType as ServiceType]}»`
        )
        return
      }
    }

    setSubmitting(true)
    try {
      await bookingApi.create({
        roomId,
        checkIn,
        checkOut,
        amenities: selectedAmenities,
      })
      navigate('/bookings/my')
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Ошибка создания брони'
      setSubmitError(typeof msg === 'string' ? msg : 'Ошибка создания брони')
    } finally {
      setSubmitting(false)
    }
  }

  if (loadingRoom) return <Spinner />

  if (roomError) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
        {roomError}
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="page-title">Создание брони</h1>

      {/* Room info card */}
      {room && (
        <div className="card mb-6">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-lg font-bold text-gray-900">
              Номер #{room.roomNumber}
            </h2>
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-semibold ${ROOM_CLASS_COLORS[room.roomClass]}`}
            >
              {ROOM_CLASS_LABELS[room.roomClass]}
            </span>
          </div>
          <div className="flex gap-4 text-sm text-gray-600">
            <span>👥 Вместимость: {room.capacity}</span>
            <span>💰 {room.pricePerNight.toLocaleString('ru-RU')} ₽/ночь</span>
          </div>
          {room.description && (
            <p className="text-sm text-gray-500 mt-2">{room.description}</p>
          )}
        </div>
      )}

      {/* Stay info */}
      <div className="card mb-6">
        <h2 className="section-title">Детали проживания</h2>
        <div className="grid grid-cols-3 gap-4 text-sm">
          <div>
            <p className="text-gray-500">Заезд</p>
            <p className="font-semibold text-gray-900">
              {checkIn
                ? new Date(checkIn).toLocaleDateString('ru-RU')
                : '—'}
            </p>
          </div>
          <div>
            <p className="text-gray-500">Выезд</p>
            <p className="font-semibold text-gray-900">
              {checkOut
                ? new Date(checkOut).toLocaleDateString('ru-RU')
                : '—'}
            </p>
          </div>
          <div>
            <p className="text-gray-500">Ночей</p>
            <p className="font-semibold text-gray-900">{nights}</p>
          </div>
        </div>
        <div className="mt-3 pt-3 border-t border-gray-100 flex justify-between text-sm">
          <span className="text-gray-500">Стоимость номера</span>
          <span className="font-semibold">
            {roomCost.toLocaleString('ru-RU')} ₽
          </span>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        {/* Amenities */}
        <div className="card mb-6">
          <h2 className="section-title">Дополнительные услуги</h2>
          <div className="space-y-4">
            {ALL_SERVICES.map((service) => (
              <div key={service} className="border border-gray-200 rounded-lg p-4">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={amenities[service].checked}
                    onChange={() => toggleAmenity(service)}
                    className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
                  />
                  <span className="font-medium text-gray-800">
                    {SERVICE_TYPE_LABELS[service]}
                  </span>
                </label>

                {amenities[service].checked && (
                  <div className="mt-3 grid grid-cols-2 gap-3 pl-7">
                    <div>
                      <label className="label" htmlFor={`${service}-start`}>
                        Начало
                      </label>
                      <input
                        id={`${service}-start`}
                        type="datetime-local"
                        className="input"
                        value={amenities[service].startTime}
                        onChange={(e) =>
                          updateAmenity(service, 'startTime', e.target.value)
                        }
                        required
                      />
                    </div>
                    <div>
                      <label className="label" htmlFor={`${service}-end`}>
                        Конец
                      </label>
                      <input
                        id={`${service}-end`}
                        type="datetime-local"
                        className="input"
                        value={amenities[service].endTime}
                        onChange={(e) =>
                          updateAmenity(service, 'endTime', e.target.value)
                        }
                        required
                      />
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Total preview */}
        <div className="card mb-6 bg-primary-50 border-primary-200">
          <div className="flex justify-between items-center">
            <span className="text-gray-700 font-medium">
              Итоговая стоимость (без доп. услуг)
            </span>
            <span className="text-xl font-bold text-primary-700">
              {roomCost.toLocaleString('ru-RU')} ₽
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Стоимость дополнительных услуг будет добавлена при оформлении
          </p>
        </div>

        {submitError && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {submitError}
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="button"
            className="btn-secondary flex-1"
            onClick={() => navigate(-1)}
          >
            Назад
          </button>
          <button
            type="submit"
            className="btn-primary flex-1"
            disabled={submitting}
          >
            {submitting ? (
              <span className="flex items-center gap-2">
                <span className="animate-spin border-2 border-white border-t-transparent rounded-full w-4 h-4" />
                Создание...
              </span>
            ) : (
              'Создать бронь'
            )}
          </button>
        </div>
      </form>
    </div>
  )
}

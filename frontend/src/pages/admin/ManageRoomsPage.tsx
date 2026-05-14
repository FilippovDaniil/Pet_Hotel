// Страница управления номерами (ADMIN): CRUD через таблицу + модальные окна.
import React, { useState, useEffect } from 'react'
import { roomApi } from '../../api/room.api'
import type { Room, RoomClass, RoomRequest } from '../../types'
import { ROOM_CLASS_LABELS, ROOM_CLASS_COLORS } from '../../types'

const ROOM_CLASSES: RoomClass[] = ['ORDINARY', 'MIDDLE', 'PREMIUM']

// Фабрика пустой формы для создания нового номера.
const defaultForm = (): RoomRequest => ({
  roomNumber: '',
  roomClass: 'ORDINARY',
  capacity: 2,
  pricePerNight: 2000,
  description: '',
})

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

// Пропсы модала: room=null → создание, room=Room → редактирование.
interface RoomModalProps {
  room: Room | null
  onClose: () => void
  onSaved: () => void  // вызывается после успешного сохранения → обновляет список
}

function RoomModal({ room, onClose, onSaved }: RoomModalProps) {
  // Предзаполняем форму из существующего номера или пустыми значениями.
  const [form, setForm] = useState<RoomRequest>(
    room
      ? {
          roomNumber: room.roomNumber,
          roomClass: room.roomClass,
          capacity: room.capacity,
          pricePerNight: room.pricePerNight,
          description: room.description ?? '',
        }
      : defaultForm()
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Универсальный обработчик: числовые поля конвертируем в Number.
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >
  ) => {
    const { name, value } = e.target
    setForm((prev) => ({
      ...prev,
      [name]:
        name === 'capacity' || name === 'pricePerNight'
          ? Number(value)
          : value,
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      // room != null → редактирование (PUT), иначе → создание (POST).
      if (room) {
        await roomApi.update(room.id, form)
      } else {
        await roomApi.create(form)
      }
      onSaved()
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.response?.data || 'Ошибка сохранения'
      setError(typeof msg === 'string' ? msg : 'Ошибка сохранения')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold text-gray-900 mb-5">
          {room ? 'Редактировать номер' : 'Добавить номер'}
        </h3>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="roomNumber">
              Номер комнаты
            </label>
            <input
              id="roomNumber"
              name="roomNumber"
              type="text"
              className="input"
              value={form.roomNumber}
              onChange={handleChange}
              required
              placeholder="101"
            />
          </div>

          <div>
            <label className="label" htmlFor="roomClass">
              Класс
            </label>
            <select
              id="roomClass"
              name="roomClass"
              className="input"
              value={form.roomClass}
              onChange={handleChange}
            >
              {ROOM_CLASSES.map((c) => (
                <option key={c} value={c}>
                  {ROOM_CLASS_LABELS[c]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="capacity">
              Вместимость
            </label>
            <input
              id="capacity"
              name="capacity"
              type="number"
              className="input"
              value={form.capacity}
              onChange={handleChange}
              min={1}
              max={20}
              required
            />
          </div>

          <div>
            <label className="label" htmlFor="pricePerNight">
              Цена за ночь (₽)
            </label>
            <input
              id="pricePerNight"
              name="pricePerNight"
              type="number"
              className="input"
              value={form.pricePerNight}
              onChange={handleChange}
              min={0}
              required
            />
          </div>

          <div>
            <label className="label" htmlFor="description">
              Описание
            </label>
            <textarea
              id="description"
              name="description"
              className="input resize-none"
              rows={3}
              value={form.description}
              onChange={handleChange}
              placeholder="Краткое описание номера..."
            />
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
              {loading ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function ManageRoomsPage() {
  const [rooms, setRooms] = useState<Room[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  // undefined = модал закрыт; 'new' = создание; Room = редактирование.
  const [editRoom, setEditRoom] = useState<Room | null | 'new'>()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  const fetchRooms = () => {
    setLoading(true)
    roomApi
      .getAll()
      .then((data) => {
        setRooms(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить номера')
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchRooms()
  }, [])

  const handleSaved = () => {
    setEditRoom(undefined)  // закрываем модал
    fetchRooms()            // обновляем список
  }

  const handleDelete = async (id: number) => {
    setDeleteLoading(true)
    try {
      await roomApi.delete(id)
      setDeleteId(null)
      fetchRooms()
    } catch {
      setError('Не удалось удалить номер')
    } finally {
      setDeleteLoading(false)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="page-title mb-0">Управление номерами</h1>
        <button
          className="btn-primary"
          onClick={() => setEditRoom('new')}
        >
          + Добавить номер
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {loading && <Spinner />}

      {!loading && rooms.length === 0 && !error && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-4xl mb-3">🏠</p>
          <p>Номера не добавлены</p>
        </div>
      )}

      {!loading && rooms.length > 0 && (
        <div className="card overflow-x-auto p-0">
          <table className="w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Номер', 'Класс', 'Вместимость', 'Цена/ночь', 'Описание', 'Действия'].map(
                  (h) => (
                    <th
                      key={h}
                      className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider"
                    >
                      {h}
                    </th>
                  )
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {rooms.map((room) => (
                <tr key={room.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-semibold text-gray-900">
                    #{room.roomNumber}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${ROOM_CLASS_COLORS[room.roomClass]}`}
                    >
                      {ROOM_CLASS_LABELS[room.roomClass]}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{room.capacity}</td>
                  <td className="px-4 py-3 font-semibold text-gray-900 whitespace-nowrap">
                    {room.pricePerNight.toLocaleString('ru-RU')} ₽
                  </td>
                  {/* truncate: обрезаем длинное описание в таблице */}
                  <td className="px-4 py-3 text-gray-500 max-w-xs truncate">
                    {room.description || '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        className="btn-secondary text-xs py-1 px-2"
                        onClick={() => setEditRoom(room)}
                      >
                        Редактировать
                      </button>
                      <button
                        className="btn-danger text-xs py-1 px-2"
                        onClick={() => setDeleteId(room.id)}  // открывает диалог подтверждения
                      >
                        Удалить
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Модал создания/редактирования номера */}
      {editRoom !== undefined && (
        <RoomModal
          room={editRoom === 'new' ? null : editRoom}
          onClose={() => setEditRoom(undefined)}
          onSaved={handleSaved}
        />
      )}

      {/* Диалог подтверждения удаления */}
      {deleteId !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-bold text-gray-900 mb-2">
              Удалить номер?
            </h3>
            <p className="text-gray-500 text-sm mb-5">
              Это действие необратимо. Номер и все связанные данные будут удалены.
            </p>
            <div className="flex gap-3">
              <button
                className="btn-secondary flex-1"
                onClick={() => setDeleteId(null)}
                disabled={deleteLoading}
              >
                Отмена
              </button>
              <button
                className="btn-danger flex-1"
                onClick={() => handleDelete(deleteId)}
                disabled={deleteLoading}
              >
                {deleteLoading ? 'Удаление...' : 'Удалить'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

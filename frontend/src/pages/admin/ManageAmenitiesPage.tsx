import React, { useState, useEffect, useRef } from 'react'
import { amenityApi } from '../../api/amenity.api'
import type { Amenity, AmenityRequest, ServiceType } from '../../types'
import { SERVICE_TYPE_LABELS } from '../../types'

const ALL_SERVICE_TYPES: ServiceType[] = [
  'SAUNA',
  'BATH',
  'POOL',
  'BILLIARD_RUS',
  'BILLIARD_US',
  'MASSAGE',
]

const defaultForm = (): AmenityRequest => ({
  name: '',
  type: 'SAUNA',
  defaultPrice: 500,
  maxDurationMinutes: 60,
  description: '',
  available: true,
})

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

interface AmenityModalProps {
  amenity: Amenity | null
  onClose: () => void
  onSaved: () => void
}

function AmenityModal({ amenity, onClose, onSaved }: AmenityModalProps) {
  const [form, setForm] = useState<AmenityRequest>(
    amenity
      ? {
          name: amenity.name,
          type: amenity.type,
          defaultPrice: amenity.defaultPrice,
          maxDurationMinutes: amenity.maxDurationMinutes,
          description: amenity.description ?? '',
          available: amenity.available,
        }
      : defaultForm()
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(
    amenity?.hasImage ? `/api/amenities/${amenity.id}/image` : null
  )
  const fileRef = useRef<HTMLInputElement>(null)

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target
    if (type === 'checkbox') {
      setForm((prev) => ({ ...prev, [name]: (e.target as HTMLInputElement).checked }))
    } else {
      setForm((prev) => ({
        ...prev,
        [name]:
          name === 'defaultPrice' || name === 'maxDurationMinutes'
            ? Number(value)
            : value,
      }))
    }
  }

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      setError('Размер изображения не должен превышать 2 МБ')
      return
    }
    setError('')
    setImageFile(file)
    setImagePreview(URL.createObjectURL(file))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      let savedId: number
      if (amenity) {
        await amenityApi.update(amenity.id, form)
        savedId = amenity.id
      } else {
        const created = await amenityApi.create(form)
        savedId = created.id
      }
      if (imageFile) {
        await amenityApi.uploadImage(savedId, imageFile)
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
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4 overflow-y-auto py-8">
      <div className="bg-white rounded-xl p-6 w-full max-w-lg shadow-xl">
        <h3 className="text-lg font-bold text-gray-900 mb-5">
          {amenity ? 'Редактировать услугу' : 'Добавить услугу'}
        </h3>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="amenity-name">
              Название
            </label>
            <input
              id="amenity-name"
              name="name"
              type="text"
              className="input"
              value={form.name}
              onChange={handleChange}
              required
              placeholder="Финская сауна"
            />
          </div>

          <div>
            <label className="label" htmlFor="amenity-type">
              Тип услуги
            </label>
            <select
              id="amenity-type"
              name="type"
              className="input"
              value={form.type}
              onChange={handleChange}
            >
              {ALL_SERVICE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {SERVICE_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label" htmlFor="amenity-price">
                Цена по умолчанию (₽)
              </label>
              <input
                id="amenity-price"
                name="defaultPrice"
                type="number"
                className="input"
                value={form.defaultPrice}
                onChange={handleChange}
                min={0.01}
                step={0.01}
                required
              />
            </div>

            <div>
              <label className="label" htmlFor="amenity-duration">
                Длительность (мин)
              </label>
              <input
                id="amenity-duration"
                name="maxDurationMinutes"
                type="number"
                className="input"
                value={form.maxDurationMinutes}
                onChange={handleChange}
                min={1}
                required
              />
            </div>
          </div>

          <div>
            <label className="label" htmlFor="amenity-desc">
              Описание
            </label>
            <textarea
              id="amenity-desc"
              name="description"
              className="input min-h-[80px] resize-y"
              value={form.description}
              onChange={handleChange}
              placeholder="Расскажите об услуге..."
              maxLength={2000}
            />
            <p className="text-xs text-gray-400 mt-1 text-right">
              {(form.description?.length ?? 0)}/2000
            </p>
          </div>

          <div>
            <label className="label">Фотография (до 2 МБ)</label>
            {imagePreview && (
              <div className="mb-2 relative inline-block">
                <img
                  src={imagePreview}
                  alt="Предпросмотр"
                  className="w-full max-h-40 object-cover rounded-lg"
                />
                <button
                  type="button"
                  className="absolute top-1 right-1 bg-white rounded-full p-1 shadow text-gray-500 hover:text-red-600"
                  onClick={() => {
                    setImagePreview(null)
                    setImageFile(null)
                    if (fileRef.current) fileRef.current.value = ''
                  }}
                >
                  ✕
                </button>
              </div>
            )}
            <input
              ref={fileRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
              onChange={handleImageChange}
            />
          </div>

          <div className="flex items-center gap-3 pt-1">
            <input
              id="amenity-available"
              name="available"
              type="checkbox"
              className="w-4 h-4 text-primary-600 rounded border-gray-300"
              checked={form.available}
              onChange={handleChange}
            />
            <label htmlFor="amenity-available" className="text-sm font-medium text-gray-700">
              Услуга доступна для заказа
            </label>
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

export default function ManageAmenitiesPage() {
  const [amenities, setAmenities] = useState<Amenity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editAmenity, setEditAmenity] = useState<Amenity | null | 'new'>()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  const fetchAmenities = () => {
    setLoading(true)
    amenityApi
      .getAll()
      .then((data) => {
        setAmenities(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить услуги')
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchAmenities()
  }, [])

  const handleSaved = () => {
    setEditAmenity(undefined)
    fetchAmenities()
  }

  const handleDelete = async (id: number) => {
    setDeleteLoading(true)
    try {
      await amenityApi.delete(id)
      setDeleteId(null)
      fetchAmenities()
    } catch {
      setError('Не удалось удалить услугу')
    } finally {
      setDeleteLoading(false)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="page-title mb-0">Управление услугами</h1>
        <button className="btn-primary" onClick={() => setEditAmenity('new')}>
          + Добавить услугу
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {loading && <Spinner />}

      {!loading && amenities.length === 0 && !error && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-4xl mb-3">🎯</p>
          <p>Услуги не добавлены</p>
        </div>
      )}

      {!loading && amenities.length > 0 && (
        <div className="card overflow-x-auto p-0">
          <table className="w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {[
                  'Фото',
                  'Название',
                  'Тип',
                  'Цена',
                  'Длит. (мин)',
                  'Доступна',
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
              {amenities.map((amenity) => (
                <tr key={amenity.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    {amenity.hasImage ? (
                      <img
                        src={`/api/amenities/${amenity.id}/image`}
                        alt={amenity.name}
                        className="w-12 h-12 object-cover rounded-lg"
                      />
                    ) : (
                      <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs">
                        Нет
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-semibold text-gray-900">{amenity.name}</div>
                    {amenity.description && (
                      <div className="text-xs text-gray-400 mt-0.5 max-w-xs truncate">
                        {amenity.description}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-xs bg-purple-100 text-purple-700 px-2.5 py-0.5 rounded-full font-medium">
                      {SERVICE_TYPE_LABELS[amenity.type] ?? amenity.type}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-semibold text-gray-900 whitespace-nowrap">
                    {amenity.defaultPrice.toLocaleString('ru-RU')} ₽
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {amenity.maxDurationMinutes}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                        amenity.available
                          ? 'bg-green-100 text-green-700'
                          : 'bg-red-100 text-red-600'
                      }`}
                    >
                      {amenity.available ? 'Да' : 'Нет'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        className="btn-secondary text-xs py-1 px-2"
                        onClick={() => setEditAmenity(amenity)}
                      >
                        Редактировать
                      </button>
                      <button
                        className="btn-danger text-xs py-1 px-2"
                        onClick={() => setDeleteId(amenity.id)}
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

      {editAmenity !== undefined && (
        <AmenityModal
          amenity={editAmenity === 'new' ? null : editAmenity}
          onClose={() => setEditAmenity(undefined)}
          onSaved={handleSaved}
        />
      )}

      {deleteId !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-bold text-gray-900 mb-2">
              Удалить услугу?
            </h3>
            <p className="text-gray-500 text-sm mb-5">
              Услуга будет удалена. Это действие необратимо.
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

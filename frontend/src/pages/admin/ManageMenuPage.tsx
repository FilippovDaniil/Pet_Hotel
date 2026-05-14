// Страница управления меню буфета (ADMIN): CRUD через таблицу + модальные окна.
import React, { useState, useEffect } from 'react'
import { diningApi } from '../../api/dining.api'
import type { MenuItem, MenuItemRequest } from '../../types'

// Пустые начальные значения для формы создания нового блюда.
const defaultForm = (): MenuItemRequest => ({
  name: '',
  category: '',
  price: 0,
  available: true,  // по умолчанию блюдо доступно
})

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

// item=null → создание нового блюда, item=MenuItem → редактирование.
interface MenuModalProps {
  item: MenuItem | null
  onClose: () => void
  onSaved: () => void
}

function MenuModal({ item, onClose, onSaved }: MenuModalProps) {
  const [form, setForm] = useState<MenuItemRequest>(
    item
      ? {
          name: item.name,
          category: item.category,
          price: item.price,
          available: item.available,
        }
      : defaultForm()
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Обрабатывает text/number inputs и checkbox: type влияет на какое поле читать.
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : name === 'price' ? Number(value) : value,
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (item) {
        await diningApi.updateMenuItem(item.id, form)  // PUT — редактирование
      } else {
        await diningApi.createMenuItem(form)            // POST — создание
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
          {item ? 'Редактировать блюдо' : 'Добавить блюдо'}
        </h3>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="menu-name">
              Название
            </label>
            <input
              id="menu-name"
              name="name"
              type="text"
              className="input"
              value={form.name}
              onChange={handleChange}
              required
              placeholder="Борщ"
            />
          </div>

          <div>
            <label className="label" htmlFor="menu-category">
              Категория
            </label>
            <input
              id="menu-category"
              name="category"
              type="text"
              className="input"
              value={form.category}
              onChange={handleChange}
              required
              placeholder="Первые блюда"
            />
          </div>

          <div>
            <label className="label" htmlFor="menu-price">
              Цена (₽)
            </label>
            <input
              id="menu-price"
              name="price"
              type="number"
              className="input"
              value={form.price}
              onChange={handleChange}
              min={0}
              step={1}
              required
            />
          </div>

          {/* Чекбокс: контролируется через checked + onChange (не value) */}
          <div className="flex items-center gap-3">
            <input
              id="menu-available"
              name="available"
              type="checkbox"
              className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
              checked={form.available}
              onChange={handleChange}
            />
            <label htmlFor="menu-available" className="text-sm font-medium text-gray-700">
              Доступно для заказа
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

export default function ManageMenuPage() {
  const [items, setItems] = useState<MenuItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  // undefined = закрыт; 'new' = создание; MenuItem = редактирование
  const [editItem, setEditItem] = useState<MenuItem | null | 'new'>()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  const fetchItems = () => {
    setLoading(true)
    diningApi
      .getMenu()
      .then((data) => {
        setItems(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить меню')
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchItems()
  }, [])

  const handleSaved = () => {
    setEditItem(undefined)
    fetchItems()
  }

  const handleDelete = async (id: number) => {
    setDeleteLoading(true)
    try {
      await diningApi.deleteMenuItem(id)
      setDeleteId(null)
      fetchItems()
    } catch {
      setError('Не удалось удалить блюдо')
    } finally {
      setDeleteLoading(false)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="page-title mb-0">Управление меню</h1>
        <button className="btn-primary" onClick={() => setEditItem('new')}>
          + Добавить блюдо
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {loading && <Spinner />}

      {!loading && items.length === 0 && !error && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-4xl mb-3">🍽️</p>
          <p>Меню пока не добавлено</p>
        </div>
      )}

      {!loading && items.length > 0 && (
        <div className="card overflow-x-auto p-0">
          <table className="w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Название', 'Категория', 'Цена', 'Доступность', 'Действия'].map(
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
              {items.map((item) => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-semibold text-gray-900">
                    {item.name}
                  </td>
                  <td className="px-4 py-3">
                    {/* Пилюля категории */}
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                      {item.category}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-semibold text-gray-900 whitespace-nowrap">
                    {item.price.toLocaleString('ru-RU')} ₽
                  </td>
                  <td className="px-4 py-3">
                    {/* Зелёный/серый бейдж доступности */}
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                        item.available
                          ? 'bg-green-100 text-green-700'
                          : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {item.available ? 'Доступно' : 'Недоступно'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        className="btn-secondary text-xs py-1 px-2"
                        onClick={() => setEditItem(item)}
                      >
                        Редактировать
                      </button>
                      <button
                        className="btn-danger text-xs py-1 px-2"
                        onClick={() => setDeleteId(item.id)}
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

      {/* Модал создания/редактирования */}
      {editItem !== undefined && (
        <MenuModal
          item={editItem === 'new' ? null : editItem}
          onClose={() => setEditItem(undefined)}
          onSaved={handleSaved}
        />
      )}

      {/* Диалог подтверждения удаления */}
      {deleteId !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-bold text-gray-900 mb-2">
              Удалить блюдо?
            </h3>
            <p className="text-gray-500 text-sm mb-5">
              Блюдо будет удалено из меню. Это действие необратимо.
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

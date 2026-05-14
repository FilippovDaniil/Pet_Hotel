// Страница меню буфета (CUSTOMER) — просмотр и заказ блюд.
import React, { useState, useEffect } from 'react'
import { diningApi } from '../../api/dining.api'
import { bookingApi } from '../../api/booking.api'
import type { MenuItem, Booking, DeliveryType } from '../../types'
import { DELIVERY_TYPE_LABELS, DELIVERY_TYPE_ICONS } from '../../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

// Пропсы модального окна заказа.
interface OrderModalProps {
  item: MenuItem         // выбранное блюдо
  bookings: Booking[]    // активные брони клиента (для привязки заказа)
  onClose: () => void    // закрыть без действий
  onSuccess: () => void  // успешный заказ
}

// Модальное окно оформления заказа: выбор брони, способа доставки, количества.
function OrderModal({ item, bookings, onClose, onSuccess }: OrderModalProps) {
  // Предзаполняем первую активную бронь.
  const [bookingId, setBookingId] = useState<number | ''>(
    bookings.length > 0 ? bookings[0].id : ''
  )
  const [quantity, setQuantity] = useState(1)
  const [deliveryType, setDeliveryType] = useState<DeliveryType>('DINING_ROOM')
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
        deliveryType,
      })
      onSuccess()  // закрывает модал и показывает уведомление об успехе
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.response?.data || 'Ошибка заказа'
      setError(typeof msg === 'string' ? msg : 'Ошибка заказа')
    } finally {
      setLoading(false)
    }
  }

  // Для отображения номера комнаты в строке итога.
  const selectedBooking = bookings.find((b) => b.id === Number(bookingId))

  return (
    // Overlay: затемнённый фон, клик вне модала не закрывает (нет onClick на overlay)
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

        {/* Если нет активных броней — показываем сообщение вместо формы */}
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
              {/* select: только активные брони (PENDING/CONFIRMED) */}
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
              {selectedBooking && (
                <p className="text-xs text-gray-500 mt-1">
                  Счёт за заказ будет добавлен к броне #{selectedBooking.id}
                </p>
              )}
            </div>

            {/* Выбор способа доставки: кнопки-карточки вместо radio */}
            <div>
              <label className="label">Способ получения</label>
              <div className="grid grid-cols-2 gap-3">
                {(['DINING_ROOM', 'ROOM_DELIVERY'] as DeliveryType[]).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setDeliveryType(type)}
                    className={`flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all ${
                      deliveryType === type
                        ? 'border-primary-500 bg-primary-50 text-primary-700'  // активный
                        : 'border-gray-200 hover:border-gray-300 text-gray-600'
                    }`}
                  >
                    <span className="text-2xl">{DELIVERY_TYPE_ICONS[type]}</span>
                    <span className="text-xs font-medium text-center">
                      {DELIVERY_TYPE_LABELS[type]}
                    </span>
                  </button>
                ))}
              </div>
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

            {/* Итоговая сводка: цена × количество */}
            <div className="bg-gray-50 rounded-lg p-3 text-sm space-y-1">
              <div className="flex justify-between">
                <span className="text-gray-500">Итого:</span>
                <strong className="text-gray-900">
                  {(item.price * quantity).toLocaleString('ru-RU')} ₽
                </strong>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-gray-400">Будет добавлено к счёту брони</span>
                <span className="text-gray-500">
                  {deliveryType === 'ROOM_DELIVERY' ? `Номер #${selectedBooking?.roomId ?? '—'}` : 'Столовая'}
                </span>
              </div>
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

// Маппинг для отображения заголовков категорий в UI.
const CATEGORY_LABELS: Record<string, string> = {
  'Завтрак': 'Завтраки',
  'Обед': 'Обед',
  'Ужин': 'Ужин',
  'Напитки': 'Напитки',
  'Десерты': 'Десерты',
}

export default function MenuPage() {
  const [menuItems, setMenuItems] = useState<MenuItem[]>([])
  const [activeBookings, setActiveBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [orderItem, setOrderItem] = useState<MenuItem | null>(null)  // null = модал закрыт
  const [successMsg, setSuccessMsg] = useState('')                   // временное сообщение об успехе

  useEffect(() => {
    // Параллельная загрузка меню и активных броней.
    Promise.all([
      diningApi.getMenu().catch(() => [] as MenuItem[]),
      bookingApi
        .getMyBookings()
        // Оставляем только активные — только к ним можно прикрепить заказ.
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

  // Уникальные категории из доступных позиций (в порядке появления в данных).
  const categories = Array.from(
    new Set(menuItems.filter((m) => m.available).map((m) => m.category))
  )

  const handleOrderSuccess = () => {
    setOrderItem(null)  // закрываем модал
    setSuccessMsg('Заказ успешно оформлен! Он добавлен к счёту вашей брони.')
    setTimeout(() => setSuccessMsg(''), 5000)  // скрываем уведомление через 5 секунд
  }

  if (loading) return <Spinner />

  return (
    <div>
      <div className="flex items-start justify-between mb-6 gap-4">
        <div>
          <h1 className="page-title mb-1">Меню буфета</h1>
          <p className="text-sm text-gray-500">
            Заказы списываются со счёта брони. Выберите доставку в номер или за столик в столовой.
          </p>
        </div>
        {/* Ссылка на историю заказов — обычный <a> т.к. маршрут /orders/my */}
        <a href="/orders/my" className="btn-secondary text-sm flex-shrink-0">
          Мои заказы →
        </a>
      </div>

      {/* Предупреждение если нет активных броней (кнопки "Заказать" будут disabled) */}
      {activeBookings.length === 0 && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 px-4 py-3 rounded-lg mb-6 text-sm">
          У вас нет активных броней. Для заказа блюд необходима активная бронь.
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {/* Временное уведомление об успехе */}
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

      {/* Меню сгруппировано по категориям */}
      {categories.map((category) => (
        <div key={category} className="mb-8">
          {/* ?? category: если нет в маппинге — показываем оригинальную строку */}
          <h2 className="section-title">{CATEGORY_LABELS[category] ?? category}</h2>
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
                      onClick={() => setOrderItem(item)}  // открывает модал с этим блюдом
                      disabled={activeBookings.length === 0}  // нельзя заказать без брони
                    >
                      Заказать
                    </button>
                  </div>
                </div>
              ))}
          </div>
        </div>
      ))}

      {/* Модал заказа: рендерится только если выбрано блюдо */}
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

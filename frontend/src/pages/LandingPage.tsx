// Главная публичная страница отеля — маркетинговый лендинг.
// Доступна без авторизации; показывает разные CTA в зависимости от isAuthenticated.
import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth.store'
import { amenityApi } from '../api/amenity.api'
import type { Amenity } from '../types'
import type { ServiceType } from '../types/booking'
import { SERVICE_TYPE_LABELS } from '../types/booking'

// Иконки-эмодзи для карточек услуг (когда нет загруженного изображения).
const SERVICE_ICONS: Record<ServiceType, string> = {
  SAUNA:        '🧖',
  BATH:         '🛁',
  POOL:         '🏊',
  BILLIARD_RUS: '🎱',
  BILLIARD_US:  '🎱',
  MASSAGE:      '💆',
}

// Заглушки описаний если у услуги не заполнено поле description.
const FALLBACK_DESCRIPTIONS: Record<ServiceType, string> = {
  SAUNA:        'Финская сауна с парилкой, бассейном для охлаждения и зоной отдыха.',
  BATH:         'Традиционная русская баня на дровах с берёзовыми вениками.',
  POOL:         'Подогреваемый бассейн с детской зоной. Работает ежедневно.',
  BILLIARD_RUS: 'Стол для русского бильярда. Кии и мелок предоставляются.',
  BILLIARD_US:  'Стол для американского пула. Кии и мелок предоставляются.',
  MASSAGE:      'Классический расслабляющий массаж от профессиональных массажистов.',
}

// Статические карточки преимуществ отеля (секция "Почему выбирают нас").
const FEATURES = [
  {
    icon: '🏠',
    title: 'Уютные номера',
    desc: '10 номеров трёх классов — Стандарт, Комфорт и Премиум. Каждый номер оснащён всем необходимым для комфортного проживания.',
  },
  {
    icon: '🍽️',
    title: 'Собственная столовая',
    desc: 'Свежее меню каждый день: завтраки, обеды, ужины, напитки и десерты. Доставка в номер или за столиком в столовой.',
  },
  {
    icon: '🎯',
    title: 'Spa и развлечения',
    desc: 'Финская сауна, русская баня, бассейн, бильярд и профессиональный массаж — всё на территории отеля.',
  },
  {
    icon: '💳',
    title: 'Удобная оплата',
    desc: 'Все услуги включаются в единый счёт брони. Прозрачная система скидок для гостей премиум-номеров.',
  },
]

export default function LandingPage() {
  const { isAuthenticated, email, role } = useAuthStore()
  const navigate = useNavigate()
  const [amenities, setAmenities] = useState<Amenity[]>([])

  // Загружаем услуги при монтировании страницы.
  // GET /api/amenities публичен → работает без авторизации.
  // .catch(() => {}) — если backend недоступен, просто не показываем секцию услуг.
  useEffect(() => {
    amenityApi
      .getAll()
      .then((data) => setAmenities(data.filter((a) => a.available)))  // только доступные
      .catch(() => {})
  }, [])  // [] — выполняется один раз при монтировании

  return (
    <div className="min-h-screen flex flex-col bg-white">
      {/* ── Шапка ── sticky top-0: прилипает к верху при скролле; z-40: поверх контента */}
      <header className="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🏨</span>
            <span className="text-lg font-bold text-primary-700">Pet Hotel</span>
          </div>
          <div className="flex items-center gap-3">
            {/* Условный рендеринг: авторизован → кнопка личного кабинета; нет → вход/регистрация */}
            {isAuthenticated ? (
              <>
                <span className="text-sm text-gray-500 hidden sm:block">{email}</span>
                <button
                  onClick={() => navigate('/dashboard')}
                  className="btn-primary text-sm"
                >
                  Личный кабинет →
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-secondary text-sm">
                  Войти
                </Link>
                <Link to="/register" className="btn-primary text-sm">
                  Зарегистрироваться
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── Hero-секция ── тёмный градиент, главный призыв к действию */}
      <section className="bg-gradient-to-br from-slate-900 via-blue-900 to-blue-800 text-white py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="text-6xl mb-6">🏨</div>
          <h1 className="text-4xl sm:text-5xl font-bold mb-4 leading-tight">
            Добро пожаловать в Pet Hotel
          </h1>
          <p className="text-xl text-blue-200 mb-8 max-w-2xl mx-auto">
            Комфортный загородный отель с собственной баней, бассейном, сауной и столовой.
            Идеальное место для отдыха от городской суеты.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            {isAuthenticated ? (
              // Авторизованный клиент → "Выбрать номер"; RECEPTION/ADMIN → "Перейти в кабинет"
              <button
                onClick={() => navigate(role === 'CUSTOMER' ? '/rooms' : '/dashboard')}
                className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-blue-900 bg-white rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
              >
                {role === 'CUSTOMER' ? 'Выбрать номер' : 'Перейти в кабинет'}
              </button>
            ) : (
              <>
                <Link
                  to="/register"
                  className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-blue-900 bg-white rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
                >
                  Забронировать номер
                </Link>
                <Link
                  to="/login"
                  className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-white border-2 border-white border-opacity-50 rounded-xl hover:bg-white hover:bg-opacity-10 transition-colors"
                >
                  Уже есть аккаунт
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* ── Статистика ── цифровые факты об отеле на синем фоне */}
      <section className="bg-primary-700 text-white py-8 px-4">
        <div className="max-w-4xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
          {/* Данные seeder: 10 номеров, 3 класса, 6 услуг, 26 блюд */}
          {[
            { value: '10', label: 'Номеров' },
            { value: '3', label: 'Класса номеров' },
            { value: '6', label: 'Доп. услуг' },
            { value: '26', label: 'Блюд в меню' },
          ].map((s) => (
            <div key={s.label}>
              <div className="text-3xl font-bold">{s.value}</div>
              <div className="text-blue-200 text-sm mt-1">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Преимущества ── 4 карточки в сетке */}
      <section className="py-16 px-4 bg-gray-50">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-2">
            Почему выбирают нас
          </h2>
          <p className="text-center text-gray-500 mb-10">
            Всё для вашего комфортного отдыха в одном месте
          </p>
          {/* lg:grid-cols-4: 4 колонки на широком экране, 2 на среднем, 1 на мобильном */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {FEATURES.map((f) => (
              <div
                key={f.title}
                className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 text-center hover:shadow-md transition-shadow"
              >
                <div className="text-4xl mb-3">{f.icon}</div>
                <h3 className="font-bold text-gray-900 mb-2">{f.title}</h3>
                <p className="text-sm text-gray-500">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Услуги ── динамическая секция из amenity-service (только если есть данные) */}
      {amenities.length > 0 && (
        <section className="py-16 px-4">
          <div className="max-w-5xl mx-auto">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-2">
              Дополнительные услуги
            </h2>
            <p className="text-center text-gray-500 mb-10">
              Доступны при бронировании номера
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {amenities.map((amenity) => {
                const type = amenity.type as ServiceType
                // Используем описание из БД, если оно есть; иначе — заглушку.
                const description = amenity.description || FALLBACK_DESCRIPTIONS[type]
                return (
                  <div
                    key={amenity.id}
                    className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow flex flex-col"
                  >
                    {/* Изображение или цветная заглушка с иконкой */}
                    {amenity.hasImage ? (
                      <div className="h-44 overflow-hidden">
                        {/* src указывает на /api/amenities/{id}/image — проксируется gateway */}
                        <img
                          src={`/api/amenities/${amenity.id}/image`}
                          alt={amenity.name}
                          className="w-full h-full object-cover"  // object-cover: заполняет контейнер без деформации
                        />
                      </div>
                    ) : (
                      <div className="h-44 bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center text-7xl">
                        {SERVICE_ICONS[type] ?? '🏨'}  {/* ?? — оператор nullish coalescing */}
                      </div>
                    )}
                    <div className="p-5 flex flex-col gap-2 flex-1">
                      <div>
                        <h3 className="font-bold text-gray-900">{amenity.name}</h3>
                        <span className="text-xs text-gray-400">
                          {SERVICE_TYPE_LABELS[type]}
                        </span>
                      </div>
                      <p className="text-sm text-gray-500 flex-1">{description}</p>
                      <div className="flex items-center justify-between pt-2 border-t border-gray-100">
                        <span className="text-xs text-gray-400">
                          до {amenity.maxDurationMinutes} мин
                        </span>
                        {/* toLocaleString('ru-RU'): форматирует число по русской локали (пробелы как разделители тысяч) */}
                        <span className="font-bold text-primary-700 text-sm">
                          от {Number(amenity.defaultPrice).toLocaleString('ru-RU')} ₽
                        </span>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </section>
      )}

      {/* ── CTA (призыв к действию) ── повторный блок внизу страницы */}
      <section className="py-16 px-4 bg-primary-700 text-white">
        <div className="max-w-2xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-4">
            Готовы к незабываемому отдыху?
          </h2>
          <p className="text-blue-200 mb-8">
            Зарегистрируйтесь и выберите номер — это займёт меньше минуты.
          </p>
          {isAuthenticated ? (
            <button
              onClick={() => navigate(role === 'CUSTOMER' ? '/rooms' : '/dashboard')}
              className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-primary-700 bg-white rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
            >
              {role === 'CUSTOMER' ? 'Выбрать номер' : 'Перейти в кабинет'}
            </button>
          ) : (
            <Link
              to="/register"
              className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-primary-700 bg-white rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
            >
              Зарегистрироваться бесплатно
            </Link>
          )}
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="bg-gray-900 text-gray-400 py-8 px-4">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-xl">🏨</span>
            <span className="font-bold text-white">Pet Hotel</span>
          </div>
          <p className="text-sm">
            © {new Date().getFullYear()} Pet Hotel — Система управления отелем
          </p>
          <div className="flex gap-4 text-sm">
            <Link to="/login" className="hover:text-white transition-colors">
              Войти
            </Link>
            <Link to="/register" className="hover:text-white transition-colors">
              Регистрация
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}

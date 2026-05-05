import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { amenityApi } from '../../api/amenity.api'
import type { Amenity } from '../../types'
import type { ServiceType } from '../../types/booking'
import { SERVICE_TYPE_LABELS } from '../../types/booking'

const SERVICE_ICONS: Record<ServiceType, string> = {
  SAUNA:        '🧖',
  BATH:         '🛁',
  POOL:         '🏊',
  BILLIARD_RUS: '🎱',
  BILLIARD_US:  '🎱',
  MASSAGE:      '💆',
}

const SERVICE_DESCRIPTIONS: Record<ServiceType, string> = {
  SAUNA:        'Финская сауна с парилкой, бассейном для охлаждения и зоной отдыха. Максимальная температура 90°C.',
  BATH:         'Традиционная русская баня на дровах с берёзовыми вениками. Идеально для восстановления после дороги.',
  POOL:         'Подогреваемый бассейн 25×10 м с детской зоной. Работает ежедневно.',
  BILLIARD_RUS: 'Стол для русского бильярда. Кии и мелок предоставляются.',
  BILLIARD_US:  'Стол для американского пула (8-ball/9-ball). Кии и мелок предоставляются.',
  MASSAGE:      'Классический расслабляющий массаж от профессиональных массажистов. Продолжительность 60 минут.',
}

const PRICE_BY_CLASS: Record<ServiceType, { ORDINARY: string; MIDDLE: string; PREMIUM: string }> = {
  SAUNA:        { ORDINARY: '2 000 ₽', MIDDLE: '1 400 ₽', PREMIUM: '1-я бесплатно' },
  BATH:         { ORDINARY: '2 000 ₽', MIDDLE: '1 400 ₽', PREMIUM: '1-я бесплатно¹' },
  POOL:         { ORDINARY: '500 ₽',   MIDDLE: '350 ₽',   PREMIUM: 'Бесплатно' },
  BILLIARD_RUS: { ORDINARY: '600 ₽',   MIDDLE: '600 ₽',   PREMIUM: '600 ₽' },
  BILLIARD_US:  { ORDINARY: '600 ₽',   MIDDLE: '600 ₽',   PREMIUM: '600 ₽' },
  MASSAGE:      { ORDINARY: '3 000 ₽', MIDDLE: '3 000 ₽', PREMIUM: '1-й бесплатно' },
}

const CLASS_COLORS = {
  ORDINARY: 'bg-gray-100 text-gray-700',
  MIDDLE:   'bg-blue-100 text-blue-700',
  PREMIUM:  'bg-amber-100 text-amber-700',
}

export default function ServicesPage() {
  const [amenities, setAmenities] = useState<Amenity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    amenityApi.getAll()
      .then(setAmenities)
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="page-title">Услуги отеля</h1>
          <p className="text-gray-500 mt-1">Дополнительные услуги добавляются при бронировании номера</p>
        </div>
        <Link to="/rooms" className="btn-primary">
          Выбрать номер
        </Link>
      </div>

      {amenities.length === 0 ? (
        <div className="card text-center py-16">
          <p className="text-4xl mb-3">🎯</p>
          <p className="text-gray-500">Услуги пока не добавлены</p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            {amenities.map((amenity) => {
              const type = amenity.type as ServiceType
              const prices = PRICE_BY_CLASS[type]
              return (
                <div key={amenity.id} className="card flex flex-col gap-4">
                  <div className="flex items-start gap-3">
                    <span className="text-4xl">{SERVICE_ICONS[type] ?? '🏨'}</span>
                    <div>
                      <h3 className="font-semibold text-gray-900 text-lg">{amenity.name}</h3>
                      <span className="text-xs text-gray-500">{SERVICE_TYPE_LABELS[type]}</span>
                    </div>
                  </div>

                  <p className="text-sm text-gray-600">{SERVICE_DESCRIPTIONS[type]}</p>

                  <div className="text-xs text-gray-500">
                    Длительность: <span className="font-medium text-gray-700">{amenity.maxDurationMinutes} мин</span>
                  </div>

                  {prices && (
                    <div className="border-t pt-3 grid grid-cols-3 gap-2 text-xs">
                      {(['ORDINARY', 'MIDDLE', 'PREMIUM'] as const).map((cls) => (
                        <div key={cls} className="text-center">
                          <span className={`inline-block px-2 py-0.5 rounded font-medium mb-1 ${CLASS_COLORS[cls]}`}>
                            {cls === 'ORDINARY' ? 'Стандарт' : cls === 'MIDDLE' ? 'Комфорт' : 'Премиум'}
                          </span>
                          <div className="font-semibold text-gray-800">{prices[cls]}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          <div className="card bg-blue-50 border-blue-200 text-sm text-blue-800">
            <p className="font-medium mb-1">Как заказать услугу?</p>
            <ol className="list-decimal list-inside space-y-1">
              <li>Найдите свободный номер на странице <Link to="/rooms" className="underline hover:text-blue-600">Номера</Link></li>
              <li>Нажмите «Забронировать» — на следующем шаге выберите нужные услуги с датой и временем</li>
              <li>Стоимость услуг рассчитается автоматически с учётом класса вашего номера</li>
            </ol>
            <p className="mt-2 text-xs text-blue-600">
              ¹ Для Premium: сауна и баня делят одну бесплатную квоту — только первая из двух бесплатна.
            </p>
          </div>
        </>
      )}
    </div>
  )
}

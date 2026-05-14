// Типы дополнительных услуг — соответствуют amenity-service API.

// ServiceType импортируется из booking.ts — единый enum для всего проекта,
// так как услуги в бронировании и услуги в каталоге ссылаются на одни и те же типы.
import { ServiceType } from './booking'

// Услуга из каталога — ответ GET /api/amenities.
// Отличается от AmenityBooking (booking.ts): это не забронированная услуга,
// а описание услуги с её параметрами по умолчанию.
export interface Amenity {
  id: number
  name: string
  type: ServiceType            // привязка к enum: SAUNA, BATH, POOL и т.д.
  defaultPrice: number         // базовая цена до применения привилегий по классу номера
  maxDurationMinutes: number   // максимальное время бронирования (для валидации в форме)
  description?: string         // необязательное описание для UI-карточки
  available: boolean           // false = услуга временно недоступна
  hasImage: boolean            // true = есть фото, загружается через GET /api/amenities/{id}/image
}

// Тело запроса POST /api/amenities и PUT /api/amenities/{id} (ADMIN).
// hasImage не передаётся — управляется отдельным endpoint POST /api/amenities/{id}/image.
export interface AmenityRequest {
  name: string
  type: ServiceType
  defaultPrice: number
  maxDurationMinutes: number
  description?: string
  available: boolean
}

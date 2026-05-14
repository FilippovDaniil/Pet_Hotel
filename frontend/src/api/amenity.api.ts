// API-клиент для amenity-service: каталог дополнительных услуг.
import client from './client'
import type { Amenity, AmenityRequest } from '../types'
import type { ServiceType } from '../types/booking'

export const amenityApi = {
  // GET /api/amenities — все услуги каталога (публично, без авторизации).
  // Публичный endpoint: gateway пропускает GET /api/amenities без JWT.
  getAll: () =>
    client.get<Amenity[]>('/amenities').then(r => r.data),

  // GET /api/amenities/type/{type} — услуга по типу (SAUNA, BATH и т.д.).
  // Используется для поиска конкретной услуги при бронировании.
  getByType: (type: ServiceType) =>
    client.get<Amenity>(`/amenities/type/${type}`).then(r => r.data),

  // POST /api/amenities — создать услугу в каталоге (ADMIN).
  create: (data: AmenityRequest) =>
    client.post<Amenity>('/amenities', data).then(r => r.data),

  // PUT /api/amenities/{id} — обновить услугу (ADMIN).
  update: (id: number, data: AmenityRequest) =>
    client.put<Amenity>(`/amenities/${id}`, data).then(r => r.data),

  // DELETE /api/amenities/{id} — удалить услугу (ADMIN).
  delete: (id: number) =>
    client.delete(`/amenities/${id}`),

  // POST /api/amenities/{id}/image — загрузить изображение услуги (ADMIN).
  // FormData + multipart/form-data: браузер сам устанавливает boundary в Content-Type,
  // но axios требует явного указания заголовка для корректного определения MIME.
  // Максимальный размер файла: 2 МБ (ограничение amenity-service).
  uploadImage: (id: number, file: File) => {
    const formData = new FormData()       // стандартный Web API для multipart-запросов
    formData.append('file', file)         // ключ 'file' должен совпадать с @RequestParam в контроллере
    return client.post(`/amenities/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

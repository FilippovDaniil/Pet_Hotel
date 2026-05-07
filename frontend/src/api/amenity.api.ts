import client from './client'
import type { Amenity, AmenityRequest } from '../types'
import type { ServiceType } from '../types/booking'

export const amenityApi = {
  getAll: () =>
    client.get<Amenity[]>('/amenities').then(r => r.data),

  getByType: (type: ServiceType) =>
    client.get<Amenity>(`/amenities/type/${type}`).then(r => r.data),

  create: (data: AmenityRequest) =>
    client.post<Amenity>('/amenities', data).then(r => r.data),

  update: (id: number, data: AmenityRequest) =>
    client.put<Amenity>(`/amenities/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    client.delete(`/amenities/${id}`),

  uploadImage: (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return client.post(`/amenities/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

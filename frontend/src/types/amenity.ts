import { ServiceType } from './booking'

export interface Amenity {
  id: number
  name: string
  type: ServiceType
  defaultPrice: number
  maxDurationMinutes: number
  description?: string
  available: boolean
  hasImage: boolean
}

export interface AmenityRequest {
  name: string
  type: ServiceType
  defaultPrice: number
  maxDurationMinutes: number
  description?: string
  available: boolean
}

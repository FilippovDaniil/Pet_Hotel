import { ServiceType } from './booking'

export interface Amenity {
  id: number
  name: string
  type: ServiceType
  defaultPrice: number
  maxDurationMinutes: number
}

export interface AmenityRequest {
  name: string
  type: ServiceType
  defaultPrice: number
  maxDurationMinutes: number
}

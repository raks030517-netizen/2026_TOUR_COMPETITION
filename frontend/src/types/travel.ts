import type { Place } from './place'

export type SearchIntent = 'TOURISM_SEARCH' | 'RESTAURANT_SEARCH' | 'COURSE_SEARCH'

export interface SearchCondition {
  intent: SearchIntent
  area: string
  tourismQuery: string
  restaurantQuery: string
  trafficRequired: boolean
  busRequired: boolean
  subwayRequired: boolean
}

export interface PartialFailure {
  provider: 'NAVER_TOURISM_SEARCH' | 'NAVER_RESTAURANT_SEARCH'
  message: string
}

export interface TravelSearchResponse {
  message: string
  condition: SearchCondition
  places: Place[]
  partialFailures: PartialFailure[]
}

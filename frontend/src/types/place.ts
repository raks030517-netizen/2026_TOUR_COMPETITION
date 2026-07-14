export type PlaceType = 'TOURISM' | 'RESTAURANT'

export interface Place {
  name: string
  type?: PlaceType
  category: string
  address: string
  roadAddress: string
  latitude: number
  longitude: number
  link: string
}

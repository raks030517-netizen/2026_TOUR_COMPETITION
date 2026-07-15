export interface TourismSearchParams {
  baseYm: string;
  signguCd: string;
  keyword: string;
  pageNo?: number;
  numOfRows?: number;
}

export interface TourismPlace {
  id: string;
  name: string;
  description: string;
  address: string;
  image: string;
  category: string;
  distance: string;
  latitude?: number;
  longitude?: number;
}

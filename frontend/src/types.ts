export type TravelPhase = "idle" | "results" | "optimizing" | "route-ready" | "travelling" | "completed";

export interface Coordinate {
  latitude: number;
  longitude: number;
}

export interface TourismPlace extends Coordinate {
  id: string;
  name: string;
  district: string;
  category: string;
  subCategory?: string;
  description: string;
  image: string;
  rank?: number;
}

export interface RouteGuide {
  instruction: string;
  distanceMeters: number;
  durationMillis: number;
  pointIndex: number;
}

export interface OptimizedRoute {
  orderedPlaces: TourismPlace[];
  path: Coordinate[];
  guides: RouteGuide[];
  totalDistanceMeters: number;
  totalDurationMillis: number;
  fallback: boolean;
  option: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: string;
}

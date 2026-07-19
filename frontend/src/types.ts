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

export interface ItineraryRequest {
  startDate: string;
  endDate: string;
  themes: string[];
  companion: string;
  transport: string;
  pace: string;
  start: Coordinate;
}

export interface TravelSignal {
  label: string;
  detail: string;
  live: boolean;
}

export interface TravelContext {
  weather: TravelSignal;
  traffic: TravelSignal;
  alerts: string[];
}

export interface ItineraryStop extends TourismPlace {
  time: string;
  stayMinutes: number;
  indoor: boolean;
  reason: string;
}

export interface ItineraryDay {
  day: number;
  date: string;
  headline: string;
  stops: ItineraryStop[];
}

export interface ItineraryPlan {
  title: string;
  summary: string;
  source: "gemma" | "guided-demo";
  context: TravelContext;
  days: ItineraryDay[];
  orderedPlaces: ItineraryStop[];
  route: OptimizedRoute;
  tips: string[];
}

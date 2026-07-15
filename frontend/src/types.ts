export type TravelPhase = "idle" | "places-ready" | "route-loading" | "route-ready" | "moving" | "arrived";
export interface Coordinate { latitude: number; longitude: number; }
export interface TourismPlace extends Coordinate { id: string; name: string; description: string; address: string; image: string; category: string; distance?: string; }
export interface RouteGuide { instruction: string; distanceMeters: number; durationMillis: number; pointIndex: number; }
export interface OptimizedRoute { orderedPlaces: TourismPlace[]; path: Coordinate[]; guides: RouteGuide[]; totalDistanceMeters: number; totalDurationMillis: number; fallback: boolean; option: string; }
export interface ChatMessage { id: string; role: "user" | "assistant"; content: string; createdAt: string; }

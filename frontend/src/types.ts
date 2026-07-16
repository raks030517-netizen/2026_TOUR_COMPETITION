export type TravelPhase = "idle" | "places-ready" | "route-loading" | "route-ready" | "moving" | "arrived";
export interface Coordinate { latitude: number; longitude: number; }
export interface TourismPlace extends Coordinate { id: string; name: string; description: string; address: string; image: string; category: string; distance?: string; }
export type RouteMode = "CAR" | "TRANSIT" | "WALK";
export type RouteSegmentType = "WALK" | "BUS" | "SUBWAY" | "DRIVE";
export interface RouteSummary { distanceMeters: number; durationSeconds: number; fare: number | null; transferCount: number | null; walkingDistanceMeters: number | null; tollFare: number | null; taxiFare: number | null; fuelPrice: number | null; }
export interface RoutePath { type: RouteSegmentType; coordinates: Coordinate[]; }
export interface RouteSegment { type: RouteSegmentType; transportName: string | null; startName: string | null; endName: string | null; durationSeconds: number | null; distanceMeters: number | null; stationCount: number | null; instruction: string | null; }
export interface RouteResponse { mode: RouteMode; summary: RouteSummary; path: RoutePath[]; segments: RouteSegment[]; warnings: string[]; }
export interface ChatMessage { id: string; role: "user" | "assistant"; content: string; createdAt: string; }

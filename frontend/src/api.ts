import type {
  ChatMessage,
  Coordinate,
  ItineraryPlan,
  ItineraryRequest,
  OptimizedRoute,
  TourismPlace,
} from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

interface ApiPlace {
  name: string;
  district?: string;
  category?: string;
  subCategory?: string;
  latitude?: number | null;
  longitude?: number | null;
  rank?: number | null;
}

export interface TourismSearchResponse {
  totalCount: number;
  pageNo: number;
  numOfRows: number;
  places: ApiPlace[];
  source?: "live" | "demo";
}

interface ApiErrorBody {
  message?: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init);
  const text = await response.text();

  if (!response.ok) {
    let message = text || "요청을 처리하지 못했습니다.";
    try {
      message = (JSON.parse(text) as ApiErrorBody).message || message;
    } catch {
      // The server can return plain text for infrastructure errors.
    }
    throw new Error(message);
  }

  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export function searchTourism(keyword: string): Promise<TourismSearchResponse> {
  const params = new URLSearchParams({
    baseYm: import.meta.env.VITE_DEFAULT_BASE_YM ?? "202504",
    signguCd: import.meta.env.VITE_DEFAULT_SIGNGU_CD ?? "26350",
    keyword,
    pageNo: "1",
    numOfRows: "12",
  });
  return request(`/api/tourism/related/search?${params.toString()}`);
}

export function optimizeRoute(start: Coordinate, places: TourismPlace[]): Promise<OptimizedRoute> {
  return request("/api/routes/optimize", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ start, places, option: "trafast" }),
  });
}

export function chat(message: string, history: ChatMessage[], context: unknown): Promise<{
  message: string;
  suggestedActions: string[];
}> {
  return request("/api/ai/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, history: history.slice(-12), context }),
  });
}

export function planItinerary(payload: ItineraryRequest): Promise<ItineraryPlan> {
  return request("/api/itineraries/plan", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function adjustItinerary(itineraryRequest: ItineraryRequest, adjustment: string): Promise<ItineraryPlan> {
  return request("/api/itineraries/adjust", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ request: itineraryRequest, adjustment }),
  });
}

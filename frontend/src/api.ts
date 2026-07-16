import type { ChatMessage, Coordinate, RouteMode, RouteResponse, TourismPlace } from "./types";
import { apiFetch } from "./authApi";
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, init);
  const text = await response.text();
  if (!response.ok) {
    let errorBody: { code?: string; message?: string } | undefined;
    try {
      errorBody = JSON.parse(text) as { code?: string; message?: string };
    } catch {
      // JSON이 아닌 외부 오류 본문은 상태 코드와 함께 일반 메시지로 처리한다.
    }
    const message = errorBody?.message ?? `요청을 처리하지 못했습니다. (${response.status})`;
    throw new Error(
      errorBody?.code ? `${message} (${errorBody.code})` : message,
    );
  }
  return text ? JSON.parse(text) as T : undefined as T;
}
export function searchTourism(keyword: string): Promise<unknown> {
  const q = new URLSearchParams({
    baseYm: import.meta.env.VITE_DEFAULT_BASE_YM ?? "202504",
    signguCd: import.meta.env.VITE_DEFAULT_SIGNGU_CD ?? "26350",
    keyword,
    pageNo: "1",
    numOfRows: "15",
  });
  return request(`/api/tourism/related/search?${q}`);
}
export function searchPlaces(keyword: string): Promise<unknown> {
  const q = new URLSearchParams({ query: `부산 ${keyword}` });
  return request(`/api/places/search?${q}`);
}
export async function findRoutes(mode: RouteMode, start: Coordinate, destination: TourismPlace): Promise<RouteResponse[]> {
  const q = new URLSearchParams({
    mode,
    startLng: String(start.longitude),
    startLat: String(start.latitude),
    endLng: String(destination.longitude),
    endLat: String(destination.latitude),
  });
  const result = await request<RouteResponse | RouteResponse[]>(`/api/routes?${q}`);
  return Array.isArray(result) ? result : [result];
}
export function chat(message: string, history: ChatMessage[], context: unknown): Promise<{message: string; suggestedActions: string[]}> {
  return request("/api/ai/chat", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ message, history: history.slice(-12), context }) });
}

export interface WeatherResponse {
  forecastDate: string;
  forecastTime: string;
  temperature: number | null;
  precipitationProbability: number | null;
  precipitationType: string | null;
  skyCondition: string | null;
  windSpeed: number | null;
  humidity: number | null;
}

export function getCurrentWeather(location: Coordinate): Promise<WeatherResponse> {
  const query = new URLSearchParams({
    latitude: String(location.latitude),
    longitude: String(location.longitude),
  });
  return request(`/api/weather/current?${query}`);
}

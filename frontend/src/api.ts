import type {ChatMessage, Coordinate, OptimizedRoute, TourismPlace} from "./types";

const BASE = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${BASE}${path}`, init);
    const text = await response.text();
    if (!response.ok) throw new Error(`${response.status}: ${text || "응답 없음"}`);
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

export function optimizeRoute(start: Coordinate, places: TourismPlace[]): Promise<OptimizedRoute> {
    return request("/api/routes/optimize", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({start, places, option: "trafast"})
    });
}

export function chat(message: string, history: ChatMessage[], context: unknown): Promise<{
    message: string;
    suggestedActions: string[]
}> {
    return request("/api/ai/chat", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({message, history: history.slice(-12), context})
    });
}

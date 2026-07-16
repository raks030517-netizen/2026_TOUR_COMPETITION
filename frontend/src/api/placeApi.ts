import { apiFetch } from '../authApi'
import type { Place } from '../types/place'

interface ApiErrorResponse {
  message?: string
}

export async function searchPlaces(query: string, signal?: AbortSignal): Promise<Place[]> {
  const searchParams = new URLSearchParams({ query })
  const response = await apiFetch(`/api/places/search?${searchParams}`, { signal })

  if (!response.ok) {
    const errorResponse = await readErrorResponse(response)
    throw new Error(errorResponse.message || '장소 검색에 실패했습니다. 잠시 후 다시 시도해 주세요.')
  }

  return response.json() as Promise<Place[]>
}

async function readErrorResponse(response: Response): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse
  } catch {
    return {}
  }
}

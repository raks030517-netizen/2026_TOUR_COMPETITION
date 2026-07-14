import { env } from '../config/env'
import type { TravelSearchResponse } from '../types/travel'

interface ApiErrorResponse {
  message?: string
}

export async function searchTravel(
  message: string,
  signal?: AbortSignal,
): Promise<TravelSearchResponse> {
  const response = await fetch(`${env.apiBaseUrl}/api/travel/search`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
    signal,
  })

  if (!response.ok) {
    const errorResponse = await readErrorResponse(response)
    throw new Error(errorResponse.message || '여행 요청을 분석하지 못했습니다. 잠시 후 다시 시도해 주세요.')
  }

  return response.json() as Promise<TravelSearchResponse>
}

async function readErrorResponse(response: Response): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse
  } catch {
    return {}
  }
}

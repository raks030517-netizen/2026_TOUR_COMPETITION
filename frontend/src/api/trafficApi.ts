import { apiFetch } from '../authApi'
import type { AviTrafficStation } from '../types/traffic'

interface ApiErrorResponse {
  message?: string
}

export async function getAviTraffic(signal?: AbortSignal): Promise<AviTrafficStation[]> {
  const response = await apiFetch('/api/traffic/avi', { signal })

  if (!response.ok) {
    const errorResponse = await readErrorResponse(response)
    throw new Error(errorResponse.message || 'AVI 교통량 정보를 불러오지 못했습니다.')
  }

  return response.json() as Promise<AviTrafficStation[]>
}

async function readErrorResponse(response: Response): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse
  } catch {
    return {}
  }
}

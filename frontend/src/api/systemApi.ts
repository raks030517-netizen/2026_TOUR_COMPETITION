import { env } from '../config/env'
import type { ConfigStatus, HealthStatus } from '../types/system'

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${env.apiBaseUrl}${path}`)

  if (!response.ok) {
    throw new Error('시스템 상태를 확인하지 못했습니다.')
  }

  return response.json() as Promise<T>
}

export const getHealth = () => getJson<HealthStatus>('/api/system/health')
export const getConfigStatus = () => getJson<ConfigStatus>('/api/system/config-status')


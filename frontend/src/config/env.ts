const DEFAULT_API_BASE_URL = 'http://localhost:8080'

function readOptionalEnv(name: 'VITE_NAVER_MAP_CLIENT_ID' | 'VITE_API_BASE_URL'): string {
  const value = import.meta.env[name]
  return typeof value === 'string' ? value.trim() : ''
}

const configuredApiBaseUrl = readOptionalEnv('VITE_API_BASE_URL') || DEFAULT_API_BASE_URL

function resolveApiBaseUrl(configuredUrl: string): string {
  const url = new URL(configuredUrl)

  // 개발 서버를 localhost 또는 내부망 IP로 열어도 인증 쿠키가 같은 사이트로
  // 처리되도록, API 호스트를 현재 페이지의 호스트와 맞춘다.
  if (import.meta.env.DEV && typeof window !== 'undefined') {
    url.hostname = window.location.hostname
  }

  return url.toString().replace(/\/$/, '')
}

let apiBaseUrl: string

try {
  apiBaseUrl = resolveApiBaseUrl(configuredApiBaseUrl)
} catch {
  throw new Error('VITE_API_BASE_URL은 올바른 URL 형식이어야 합니다.')
}

export const env = Object.freeze({
  naverMapClientId: readOptionalEnv('VITE_NAVER_MAP_CLIENT_ID'),
  apiBaseUrl,
})

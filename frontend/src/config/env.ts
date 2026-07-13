const DEFAULT_API_BASE_URL = 'http://localhost:8080'

function readOptionalEnv(name: 'VITE_NAVER_MAP_CLIENT_ID' | 'VITE_API_BASE_URL'): string {
  const value = import.meta.env[name]
  return typeof value === 'string' ? value.trim() : ''
}

const apiBaseUrl = readOptionalEnv('VITE_API_BASE_URL') || DEFAULT_API_BASE_URL

try {
  new URL(apiBaseUrl)
} catch {
  throw new Error('VITE_API_BASE_URL은 올바른 URL 형식이어야 합니다.')
}

export const env = Object.freeze({
  naverMapClientId: readOptionalEnv('VITE_NAVER_MAP_CLIENT_ID'),
  apiBaseUrl,
})


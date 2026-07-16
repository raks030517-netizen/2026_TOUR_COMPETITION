import { env } from './config/env'

export interface AuthUser { id: number; email: string; displayName: string }
export interface SignupInput { email: string; password: string; displayName: string }

export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

let csrf: { headerName: string; token: string } | undefined

async function ensureCsrf() {
  if (csrf) return csrf
  const response = await fetch(`${env.apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
  if (!response.ok) throw await toError(response)
  csrf = await response.json() as { headerName: string; token: string }
  return csrf
}

export async function apiFetch(path: string, init: RequestInit = {}) {
  const method = (init.method ?? 'GET').toUpperCase()
  const requiresCsrf = !['GET', 'HEAD', 'OPTIONS'].includes(method)

  async function execute(refreshCsrf = false) {
    const headers = new Headers(init.headers)

    if (requiresCsrf) {
      if (refreshCsrf) csrf = undefined
      const token = await ensureCsrf()
      headers.set(token.headerName, token.token)
    }

    return fetch(`${env.apiBaseUrl}${path}`, { ...init, headers, credentials: 'include' })
  }

  let response = await execute()
  if (requiresCsrf && response.status === 403) {
    response = await execute(true)
  }
  return response
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, init)
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function toError(response: Response) {
  let body: { message?: string; code?: string } = {}
  try { body = await response.json() as typeof body } catch { /* empty response */ }
  return new ApiError(body.message ?? `요청 실패 (${response.status})`, response.status, body.code)
}

export const getMe = () => request<AuthUser>('/api/auth/me')
export const login = (email: string, password: string) => request<AuthUser>('/api/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }),
})
export const signup = (input: SignupInput) => request<AuthUser>('/api/auth/signup', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
})
export async function logout() {
  await request<void>('/api/auth/logout', { method: 'POST' })
  csrf = undefined
}
export const checkEmail = (email: string) => request<{ email: string; available: boolean }>(
  `/api/auth/check-email?${new URLSearchParams({ email })}`,
)

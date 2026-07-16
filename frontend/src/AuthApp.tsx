import { useEffect, useState } from 'react'
import App from './App'
import AuthPage from './components/AuthPage'
import { ApiError, getMe, login, logout, signup } from './authApi'
import type { AuthUser, SignupInput } from './authApi'
import './auth.css'

export default function AuthApp() {
  const [user, setUser] = useState<AuthUser>()
  const [checking, setChecking] = useState(true)
  const [loading, setLoading] = useState(false)
  const [mode, setMode] = useState<'login' | 'signup'>('login')
  const [error, setError] = useState('')

  useEffect(() => { getMe().then(setUser).catch(e => {
    if (!(e instanceof ApiError) || e.status !== 401) setError(e.message)
  }).finally(() => setChecking(false)) }, [])

  async function onLogin(email: string, password: string) {
    setLoading(true); setError('')
    try { setUser(await login(email, password)) } catch (e) { setError(e instanceof Error ? e.message : '로그인 실패') }
    finally { setLoading(false) }
  }
  async function onSignup(input: SignupInput) {
    setLoading(true); setError('')
    try { await signup(input); setUser(await login(input.email, input.password)) }
    catch (e) { setError(e instanceof Error ? e.message : '회원가입 실패') }
    finally { setLoading(false) }
  }
  async function onLogout() { try { await logout() } finally { setUser(undefined); setMode('login') } }

  if (checking) return <main className="auth-page">로그인 상태를 확인하는 중입니다.</main>
  if (!user) return <AuthPage mode={mode} loading={loading} error={error} onModeChange={setMode} onLogin={onLogin} onSignup={onSignup} />
  return <div><header className="account-bar"><span><b>{user.displayName}</b> · {user.email}</span><button onClick={onLogout}>로그아웃</button></header><App /></div>
}

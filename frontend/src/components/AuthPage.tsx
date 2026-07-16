import { useState } from 'react'
import type { FormEvent } from 'react'
import { checkEmail } from '../authApi'
import type { SignupInput } from '../authApi'

interface Props {
  mode: 'login' | 'signup'; loading: boolean; error: string
  onModeChange: (mode: 'login' | 'signup') => void
  onLogin: (email: string, password: string) => Promise<void>
  onSignup: (input: SignupInput) => Promise<void>
}

export default function AuthPage({ mode, loading, error, onModeChange, onLogin, onSignup }: Props) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [emailStatus, setEmailStatus] = useState('')

  async function verifyEmail() {
    if (!email.includes('@')) return setEmailStatus('')
    try {
      const result = await checkEmail(email)
      setEmailStatus(result.available ? '사용할 수 있는 이메일입니다.' : '이미 사용 중인 이메일입니다.')
    } catch { setEmailStatus('중복 여부를 확인하지 못했습니다.') }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (mode === 'signup') await onSignup({ email, password, displayName })
    else await onLogin(email, password)
  }

  return <main className="auth-page"><section className="auth-card">
    <div className="auth-brand">✦ <strong>ROAMATE</strong></div>
    <h1>{mode === 'login' ? '다시 여행을 시작하세요' : '부산 여행 계정을 만드세요'}</h1>
    <p>일정과 여행 기록을 사용자 계정에 안전하게 연결합니다.</p>
    <div className="auth-tabs">
      <button className={mode === 'login' ? 'selected' : ''} onClick={() => onModeChange('login')}>로그인</button>
      <button className={mode === 'signup' ? 'selected' : ''} onClick={() => onModeChange('signup')}>회원가입</button>
    </div>
    <form onSubmit={submit}>
      {mode === 'signup' && <label>표시 이름<input value={displayName} onChange={e => setDisplayName(e.target.value)} minLength={2} maxLength={80} required /></label>}
      <label>이메일<input type="email" value={email} onChange={e => setEmail(e.target.value)} onBlur={mode === 'signup' ? verifyEmail : undefined} required /></label>
      {mode === 'signup' && emailStatus && <small>{emailStatus}</small>}
      <label>비밀번호<input type="password" value={password} onChange={e => setPassword(e.target.value)} minLength={8} maxLength={72} required /></label>
      {error && <div className="auth-error" role="alert">{error}</div>}
      <button className="auth-submit" disabled={loading}>{loading ? '처리 중...' : mode === 'login' ? '로그인' : '회원가입하고 시작하기'}</button>
    </form>
  </section></main>
}

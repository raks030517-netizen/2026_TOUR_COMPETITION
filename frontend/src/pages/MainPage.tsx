import ChatPanel from '../components/chat/ChatPanel'
import NaverMap from '../components/map/NaverMap'

export default function MainPage() {
  return (
    <main className="main-page">
      <header>
        <h1>부산 AI 지도</h1>
      </header>
      <NaverMap />
      <ChatPanel />
    </main>
  )
}


import { useState } from 'react'
import type { FormEvent } from 'react'

export default function ChatPanel() {
  const [query, setQuery] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    // 추후 이 지점에서 POST /api/chat 요청을 연결합니다.
  }

  return (
    <section className="chat-panel" aria-labelledby="chat-title">
      <h2 id="chat-title">어떤 장소를 찾고 있나요?</h2>
      <form onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="travel-query">
          관광지 또는 음식점 검색 문장
        </label>
        <input
          id="travel-query"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="예: 해운대 근처 돼지국밥집을 알려줘."
        />
        <button type="submit">검색</button>
      </form>
    </section>
  )
}


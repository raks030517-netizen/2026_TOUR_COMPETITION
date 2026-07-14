import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { searchTravel } from '../../api/travelApi'
import type { Place } from '../../types/place'
import type { PartialFailure } from '../../types/travel'

interface TravelSearchPanelProps {
  active: boolean
  onSearchStart: () => void
  onResultsChange: (places: Place[]) => void
}

export default function TravelSearchPanel({
  active,
  onSearchStart,
  onResultsChange,
}: TravelSearchPanelProps) {
  const [message, setMessage] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [resultMessage, setResultMessage] = useState('')
  const [partialFailures, setPartialFailures] = useState<PartialFailure[]>([])
  const requestControllerRef = useRef<AbortController | null>(null)

  useEffect(() => () => requestControllerRef.current?.abort(), [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedMessage = message.trim()

    if (!trimmedMessage) {
      setErrorMessage('원하는 여행 장소를 자연어로 입력해 주세요.')
      return
    }

    requestControllerRef.current?.abort()
    const requestController = new AbortController()
    requestControllerRef.current = requestController
    onSearchStart()
    setIsLoading(true)
    setErrorMessage('')
    setResultMessage('')
    setPartialFailures([])

    try {
      const response = await searchTravel(trimmedMessage, requestController.signal)
      setResultMessage(response.message)
      setPartialFailures(response.partialFailures)
      onResultsChange(response.places)
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === 'AbortError') return
      setErrorMessage(error instanceof Error ? error.message : '여행 검색 중 오류가 발생했습니다.')
    } finally {
      if (requestControllerRef.current === requestController) {
        requestControllerRef.current = null
        setIsLoading(false)
      }
    }
  }

  return (
    <section className="travel-search-panel" aria-labelledby="travel-search-title">
      <h2 id="travel-search-title">자연어 여행 검색</h2>
      <form className="place-search-form travel-search-form" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="travel-search-message">
          원하는 관광지와 음식점
        </label>
        <textarea
          id="travel-search-message"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="예: 광안리에서 바다를 보고 조개구이를 먹고 싶어요."
          rows={3}
          disabled={isLoading}
        />
        <button type="submit" disabled={isLoading}>
          {isLoading ? '분석·검색 중…' : '자연어로 검색'}
        </button>
      </form>
      {active && errorMessage && <p className="search-message error">{errorMessage}</p>}
      {active && resultMessage && <p className="travel-result-message">{resultMessage}</p>}
      {active && partialFailures.length > 0 && (
        <ul className="partial-failure-list" aria-label="일부 검색 실패">
          {partialFailures.map((failure) => (
            <li key={failure.provider}>{failure.message}</li>
          ))}
        </ul>
      )}
    </section>
  )
}

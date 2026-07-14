import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { searchPlaces } from '../../api/placeApi'
import type { Place } from '../../types/place'

interface PlaceSearchPanelProps {
  active: boolean
  onSearchStart: () => void
  onResultsChange: (places: Place[]) => void
}

export default function PlaceSearchPanel({
  active,
  onSearchStart,
  onResultsChange,
}: PlaceSearchPanelProps) {
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [noResults, setNoResults] = useState(false)
  const requestControllerRef = useRef<AbortController | null>(null)

  useEffect(() => () => requestControllerRef.current?.abort(), [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedQuery = query.trim()

    if (!trimmedQuery) {
      setErrorMessage('검색어를 입력해 주세요.')
      return
    }

    requestControllerRef.current?.abort()
    const requestController = new AbortController()
    requestControllerRef.current = requestController
    onSearchStart()
    setIsLoading(true)
    setErrorMessage('')
    setNoResults(false)

    try {
      const results = await searchPlaces(trimmedQuery, requestController.signal)
      setNoResults(results.length === 0)
      onResultsChange(results)
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === 'AbortError') return
      setErrorMessage(error instanceof Error ? error.message : '장소 검색 중 오류가 발생했습니다.')
    } finally {
      if (requestControllerRef.current === requestController) {
        requestControllerRef.current = null
        setIsLoading(false)
      }
    }
  }

  return (
    <section className="place-search-panel" aria-labelledby="place-search-title">
      <h2 id="place-search-title">일반 장소 검색</h2>
      <form className="place-search-form" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="place-query">
          관광지 또는 음식점 검색어
        </label>
        <input
          id="place-query"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="예: 부산 광안리 조개구이"
          disabled={isLoading}
        />
        <button type="submit" disabled={isLoading}>
          {isLoading ? '검색 중…' : '검색'}
        </button>
      </form>
      {active && errorMessage && <p className="search-message error">{errorMessage}</p>}
      {active && !isLoading && !errorMessage && noResults && (
        <p className="search-message">검색 결과가 없습니다.</p>
      )}
    </section>
  )
}

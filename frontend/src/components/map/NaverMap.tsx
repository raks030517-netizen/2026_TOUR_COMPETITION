import { useEffect, useRef, useState } from 'react'
import { loadNaverMaps } from '../../lib/naverMaps'

export default function NaverMap() {
  const containerRef = useRef<HTMLDivElement>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let active = true

    loadNaverMaps()
      .then(() => {
        if (!active || !containerRef.current) return

        new window.naver.maps.Map(containerRef.current, {
          center: new window.naver.maps.LatLng(35.1796, 129.0756),
          zoom: 13,
        })
      })
      .catch((error: unknown) => {
        if (!active) return
        setErrorMessage(error instanceof Error ? error.message : '지도를 표시하지 못했습니다.')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <section className="map-panel" aria-labelledby="map-title">
      <h2 id="map-title" className="sr-only">
        네이버 지도
      </h2>
      <div ref={containerRef} className="naver-map" />
      {errorMessage && <p className="map-error">{errorMessage}</p>}
    </section>
  )
}

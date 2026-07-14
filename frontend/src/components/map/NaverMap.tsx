import { useEffect, useRef, useState } from 'react'
import { env } from '../../config/env'

interface NaverMapsApi {
  maps: {
    LatLng: new (latitude: number, longitude: number) => unknown
    Map: new (
      element: HTMLElement,
      options: { center: unknown; zoom: number },
    ) => unknown
  }
}

declare global {
  interface Window {
    naver?: NaverMapsApi
  }
}

let sdkPromise: Promise<NaverMapsApi> | undefined

function loadNaverMaps(clientId: string): Promise<NaverMapsApi> {
  if (window.naver) return Promise.resolve(window.naver)

  sdkPromise ??= new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`
    script.async = true
    script.dataset.naverMapsSdk = 'true'

    script.onload = () => {
      if (window.naver) {
        resolve(window.naver)
      } else {
        reject(new Error('네이버 지도 SDK를 초기화하지 못했습니다.'))
      }
    }
    script.onerror = () => {
      reject(new Error('네이버 지도 SDK를 불러오지 못했습니다. 키와 Web 서비스 URL을 확인해 주세요.'))
    }

    document.head.appendChild(script)
  })

  return sdkPromise
}

export default function NaverMap() {
  const containerRef = useRef<HTMLDivElement>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!env.naverMapClientId) {
      setErrorMessage('VITE_NAVER_MAP_CLIENT_ID가 설정되지 않았습니다.')
      return
    }

    let active = true

    loadNaverMaps(env.naverMapClientId)
      .then((naver) => {
        if (!active || !containerRef.current) return

        new naver.maps.Map(containerRef.current, {
          center: new naver.maps.LatLng(35.1796, 129.0756),
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

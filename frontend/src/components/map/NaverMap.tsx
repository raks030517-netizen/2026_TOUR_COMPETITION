import { useEffect, useRef, useState } from 'react'
import { env } from '../../config/env'
import type { Place, PlaceType } from '../../types/place'
import type { AviTrafficStation } from '../../types/traffic'

interface NaverLatLng {}
interface NaverEventListener {}
interface NaverSize {}
interface NaverPoint {}

interface NaverMapInstance {
  panTo(position: NaverLatLng): void
  fitBounds(bounds: NaverLatLngBounds): void
}

interface NaverLatLngBounds {
  extend(position: NaverLatLng): void
}

interface NaverMarker {
  getPosition(): NaverLatLng
  setMap(map: NaverMapInstance | null): void
}

interface NaverInfoWindow {
  open(map: NaverMapInstance, marker: NaverMarker): void
  close(): void
}

interface NaverMapsApi {
  maps: {
    LatLng: new (latitude: number, longitude: number) => NaverLatLng
    LatLngBounds: new () => NaverLatLngBounds
    Size: new (width: number, height: number) => NaverSize
    Point: new (x: number, y: number) => NaverPoint
    Map: new (
      element: HTMLElement,
      options: { center: NaverLatLng; zoom: number },
    ) => NaverMapInstance
    Marker: new (options: {
      map: NaverMapInstance
      position: NaverLatLng
      title: string
      icon?: {
        content: string
        anchor: NaverPoint
      }
    }) => NaverMarker
    InfoWindow: new (options: {
      content: HTMLElement
      borderWidth: number
      backgroundColor: string
      anchorSize: NaverSize
      pixelOffset: NaverPoint
    }) => NaverInfoWindow
    Event: {
      addListener(
        target: NaverMarker,
        eventName: 'click',
        listener: () => void,
      ): NaverEventListener
      removeListener(listener: NaverEventListener): void
    }
  }
}

interface NaverMapProps {
  places: Place[]
  selectedPlaceIndex: number | null
  onSelectPlace: (index: number) => void
  aviStations: AviTrafficStation[]
}

declare global {
  interface Window {
    naver?: NaverMapsApi
  }
}

let sdkPromise: Promise<NaverMapsApi> | undefined

const TYPE_LABEL: Record<PlaceType, string> = {
  TOURISM: '관광지',
  RESTAURANT: '음식점',
}

function loadNaverMaps(clientId: string): Promise<NaverMapsApi> {
  if (window.naver) return Promise.resolve(window.naver)

  sdkPromise ??= new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>('script[data-naver-maps-sdk]')
    if (existingScript) {
      existingScript.addEventListener('load', () => {
        if (window.naver) resolve(window.naver)
        else reject(new Error('네이버 지도 SDK를 초기화하지 못했습니다.'))
      })
      existingScript.addEventListener('error', () => reject(new Error('네이버 지도 SDK를 불러오지 못했습니다.')))
      return
    }

    const script = document.createElement('script')
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`
    script.async = true
    script.dataset.naverMapsSdk = 'true'

    script.onload = () => {
      if (window.naver) resolve(window.naver)
      else reject(new Error('네이버 지도 SDK를 초기화하지 못했습니다.'))
    }
    script.onerror = () => {
      reject(new Error('네이버 지도 SDK를 불러오지 못했습니다. 네이버 Cloud의 Web 서비스 URL을 확인해 주세요.'))
    }

    document.head.appendChild(script)
  })

  return sdkPromise
}

function createInfoWindowContent(place: Place): HTMLElement {
  const content = document.createElement('div')
  content.className = 'map-info-window'

  const name = document.createElement('strong')
  name.textContent = place.name
  content.appendChild(name)

  if (place.type) {
    const type = document.createElement('span')
    type.className = `place-type-badge ${place.type.toLowerCase()}`
    type.textContent = TYPE_LABEL[place.type]
    content.appendChild(type)
  }

  if (place.category) {
    const category = document.createElement('span')
    category.textContent = place.category
    content.appendChild(category)
  }

  const address = place.roadAddress || place.address
  if (address) {
    const addressElement = document.createElement('span')
    addressElement.textContent = address
    content.appendChild(addressElement)
  }

  if (place.link) {
    const link = document.createElement('a')
    link.href = place.link
    link.target = '_blank'
    link.rel = 'noreferrer'
    link.textContent = '네이버 상세 보기'
    content.appendChild(link)
  }

  return content
}

function createMarkerIcon(naver: NaverMapsApi, type: PlaceType | undefined) {
  if (!type) return undefined

  const label = type === 'TOURISM' ? '관' : '맛'
  return {
    content: `<span class="map-place-marker ${type.toLowerCase()}" aria-label="${TYPE_LABEL[type]}"><span>${label}</span></span>`,
    anchor: new naver.maps.Point(18, 18),
  }
}

function createAviInfoWindowContent(station: AviTrafficStation): HTMLElement {
  const content = document.createElement('div')
  content.className = 'map-info-window avi-info-window'

  const title = document.createElement('strong')
  title.textContent = 'AVI 지점 교통량'
  content.appendChild(title)

  const stationName = document.createElement('span')
  stationName.textContent = station.stationName || '지점명 없음'
  content.appendChild(stationName)

  const measuredAt = document.createElement('span')
  measuredAt.textContent = `측정 시각: ${station.measuredAt || '정보 없음'}`
  content.appendChild(measuredAt)

  const volume = document.createElement('span')
  volume.textContent = `교통량: ${station.trafficVolume.toLocaleString('ko-KR')}`
  content.appendChild(volume)

  return content
}

function createAviMarkerIcon(naver: NaverMapsApi) {
  return {
    content: '<span class="map-avi-marker" aria-label="AVI 지점">AVI</span>',
    anchor: new naver.maps.Point(21, 21),
  }
}

export default function NaverMap({
  places,
  selectedPlaceIndex,
  onSelectPlace,
  aviStations,
}: NaverMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const naverApiRef = useRef<NaverMapsApi | null>(null)
  const mapRef = useRef<NaverMapInstance | null>(null)
  const markersRef = useRef<NaverMarker[]>([])
  const infoWindowsRef = useRef<NaverInfoWindow[]>([])
  const markerListenersRef = useRef<NaverEventListener[]>([])
  const aviMarkersRef = useRef<NaverMarker[]>([])
  const aviInfoWindowsRef = useRef<NaverInfoWindow[]>([])
  const aviMarkerListenersRef = useRef<NaverEventListener[]>([])
  const onSelectPlaceRef = useRef(onSelectPlace)
  const [mapReady, setMapReady] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  onSelectPlaceRef.current = onSelectPlace

  function clearPlaceOverlays() {
    const naver = naverApiRef.current
    if (naver) {
      markerListenersRef.current.forEach((listener) => naver.maps.Event.removeListener(listener))
    }
    infoWindowsRef.current.forEach((infoWindow) => infoWindow.close())
    markersRef.current.forEach((marker) => marker.setMap(null))
    markerListenersRef.current = []
    infoWindowsRef.current = []
    markersRef.current = []
  }

  function clearAviOverlays() {
    const naver = naverApiRef.current
    if (naver) {
      aviMarkerListenersRef.current.forEach((listener) => naver.maps.Event.removeListener(listener))
    }
    aviInfoWindowsRef.current.forEach((infoWindow) => infoWindow.close())
    aviMarkersRef.current.forEach((marker) => marker.setMap(null))
    aviMarkerListenersRef.current = []
    aviInfoWindowsRef.current = []
    aviMarkersRef.current = []
  }

  useEffect(() => {
    if (!env.naverMapClientId) {
      setErrorMessage('VITE_NAVER_MAP_CLIENT_ID가 설정되지 않았습니다.')
      return
    }

    let active = true

    loadNaverMaps(env.naverMapClientId)
      .then((naver) => {
        if (!active || !containerRef.current) return

        naverApiRef.current = naver
        mapRef.current = new naver.maps.Map(containerRef.current, {
          center: new naver.maps.LatLng(35.1796, 129.0756),
          zoom: 13,
        })
        setMapReady(true)
      })
      .catch((error: unknown) => {
        if (!active) return
        setErrorMessage(error instanceof Error ? error.message : '지도를 표시하지 못했습니다.')
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!mapReady || !naverApiRef.current || !mapRef.current) return

    const naver = naverApiRef.current
    const map = mapRef.current
    clearPlaceOverlays()

    if (places.length === 0) return

    places.forEach((place, index) => {
      const position = new naver.maps.LatLng(place.latitude, place.longitude)
      const markerOptions = {
        map,
        position,
        title: place.name,
        ...(place.type ? { icon: createMarkerIcon(naver, place.type) } : {}),
      }
      const marker = new naver.maps.Marker(markerOptions)
      const infoWindow = new naver.maps.InfoWindow({
        content: createInfoWindowContent(place),
        borderWidth: 0,
        backgroundColor: 'transparent',
        anchorSize: new naver.maps.Size(12, 12),
        pixelOffset: new naver.maps.Point(0, -10),
      })
      const listener = naver.maps.Event.addListener(marker, 'click', () => {
        onSelectPlaceRef.current(index)
      })

      markersRef.current.push(marker)
      infoWindowsRef.current.push(infoWindow)
      markerListenersRef.current.push(listener)
    })

    if (markersRef.current.length === 1) {
      map.panTo(markersRef.current[0].getPosition())
    } else {
      const bounds = new naver.maps.LatLngBounds()
      markersRef.current.forEach((marker) => bounds.extend(marker.getPosition()))
      map.fitBounds(bounds)
    }

    return clearPlaceOverlays
  }, [mapReady, places])

  useEffect(() => {
    if (!mapReady || !mapRef.current) return

    infoWindowsRef.current.forEach((infoWindow) => infoWindow.close())
    if (selectedPlaceIndex === null) return

    const marker = markersRef.current[selectedPlaceIndex]
    const infoWindow = infoWindowsRef.current[selectedPlaceIndex]
    if (!marker || !infoWindow) return

    mapRef.current.panTo(marker.getPosition())
    infoWindow.open(mapRef.current, marker)
  }, [mapReady, selectedPlaceIndex])

  useEffect(() => {
    if (!mapReady || !naverApiRef.current || !mapRef.current) return

    const naver = naverApiRef.current
    const map = mapRef.current
    clearAviOverlays()

    aviStations.forEach((station) => {
      const marker = new naver.maps.Marker({
        map,
        position: new naver.maps.LatLng(station.latitude, station.longitude),
        title: station.stationName,
        icon: createAviMarkerIcon(naver),
      })
      const infoWindow = new naver.maps.InfoWindow({
        content: createAviInfoWindowContent(station),
        borderWidth: 0,
        backgroundColor: 'transparent',
        anchorSize: new naver.maps.Size(12, 12),
        pixelOffset: new naver.maps.Point(0, -10),
      })
      const listener = naver.maps.Event.addListener(marker, 'click', () => {
        aviInfoWindowsRef.current.forEach((currentInfoWindow) => currentInfoWindow.close())
        infoWindow.open(map, marker)
      })

      aviMarkersRef.current.push(marker)
      aviInfoWindowsRef.current.push(infoWindow)
      aviMarkerListenersRef.current.push(listener)
    })

    return clearAviOverlays
  }, [aviStations, mapReady])

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

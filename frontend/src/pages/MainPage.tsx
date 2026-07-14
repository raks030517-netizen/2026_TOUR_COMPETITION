import { useRef, useState } from 'react'
import NaverMap from '../components/map/NaverMap'
import PlaceResultList from '../components/place/PlaceResultList'
import PlaceSearchPanel from '../components/place/PlaceSearchPanel'
import TravelSearchPanel from '../components/place/TravelSearchPanel'
import AviTrafficToggle from '../components/traffic/AviTrafficToggle'
import type { Place } from '../types/place'
import type { AviTrafficStation } from '../types/traffic'

type SearchMode = 'TRAVEL' | 'GENERAL'

export default function MainPage() {
  const [places, setPlaces] = useState<Place[]>([])
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null)
  const [activeMode, setActiveMode] = useState<SearchMode>('TRAVEL')
  const [aviStations, setAviStations] = useState<AviTrafficStation[]>([])
  const activeModeRef = useRef<SearchMode>('TRAVEL')

  function startSearch(mode: SearchMode) {
    activeModeRef.current = mode
    setActiveMode(mode)
    setPlaces([])
    setSelectedIndex(null)
  }

  function finishSearch(mode: SearchMode, results: Place[]) {
    if (activeModeRef.current !== mode) return
    setPlaces(results)
    setSelectedIndex(null)
  }

  return (
    <main className="main-page">
      <header>
        <h1>부산 여행 지도</h1>
        <p>자연어로 관광지와 음식점을 함께 찾거나, 장소를 직접 검색해 보세요.</p>
      </header>
      <div className="search-map-layout">
        <aside className="search-sidebar" aria-label="장소 검색 및 결과">
          <TravelSearchPanel
            active={activeMode === 'TRAVEL'}
            onSearchStart={() => startSearch('TRAVEL')}
            onResultsChange={(results) => finishSearch('TRAVEL', results)}
          />
          <PlaceSearchPanel
            active={activeMode === 'GENERAL'}
            onSearchStart={() => startSearch('GENERAL')}
            onResultsChange={(results) => finishSearch('GENERAL', results)}
          />
          <AviTrafficToggle onStationsChange={setAviStations} />
          <PlaceResultList
            places={places}
            selectedIndex={selectedIndex}
            onSelectPlace={setSelectedIndex}
          />
        </aside>
        <NaverMap
          places={places}
          selectedPlaceIndex={selectedIndex}
          onSelectPlace={setSelectedIndex}
          aviStations={aviStations}
        />
      </div>
    </main>
  )
}

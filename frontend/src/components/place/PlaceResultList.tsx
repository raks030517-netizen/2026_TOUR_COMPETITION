import type { Place } from '../../types/place'

const TYPE_LABEL = {
  TOURISM: '관광지',
  RESTAURANT: '음식점',
} as const

interface PlaceResultListProps {
  places: Place[]
  selectedIndex: number | null
  onSelectPlace: (index: number) => void
}

export default function PlaceResultList({
  places,
  selectedIndex,
  onSelectPlace,
}: PlaceResultListProps) {
  if (places.length === 0) return null

  return (
    <div className="place-results" aria-live="polite">
      <p className="result-count">검색 결과 {places.length}개</p>
      <ol className="place-result-list">
        {places.map((place, index) => {
          const displayAddress = place.roadAddress || place.address
          const isSelected = selectedIndex === index

          return (
            <li key={`${place.name}-${place.latitude}-${place.longitude}`}>
              <article className={`place-result-card${isSelected ? ' selected' : ''}`}>
                <button
                  type="button"
                  className="place-card-button"
                  aria-pressed={isSelected}
                  onClick={() => onSelectPlace(index)}
                >
                  <span className="place-card-heading">
                    <strong>{place.name}</strong>
                    {place.type && (
                      <span className={`place-type-badge ${place.type.toLowerCase()}`}>
                        {TYPE_LABEL[place.type]}
                      </span>
                    )}
                  </span>
                  {place.category && <span className="place-category">{place.category}</span>}
                  {displayAddress && <span className="place-address">{displayAddress}</span>}
                </button>
                {place.link && (
                  <a href={place.link} target="_blank" rel="noreferrer">
                    네이버 상세 보기
                  </a>
                )}
              </article>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

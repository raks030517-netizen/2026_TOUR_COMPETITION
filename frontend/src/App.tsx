import { useCallback, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { optimizeRoute, searchTourism } from "./api";
import ChatBottomSheet from "./components/ChatBottomSheet";
import RoamateMap from "./components/RoamateMap";
import type { Coordinate, OptimizedRoute, TourismPlace, TravelPhase } from "./types";
import "./styles.css";

const BUSAN_CITY_HALL: Coordinate = { latitude: 35.1795543, longitude: 129.0756416 };

const CATEGORIES = ["추천", "바다", "시장", "카페", "문화", "체험", "공원", "야경"];

const IMAGES = [
  "https://images.unsplash.com/photo-1517154421773-0529f29ea451?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1588416936097-41850ab3d86d?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1000&q=80",
];

function toPlaces(response: Awaited<ReturnType<typeof searchTourism>>): TourismPlace[] {
  const result: TourismPlace[] = [];

  (response.places ?? []).forEach((place, index) => {
    const latitude = Number(place.latitude);
    const longitude = Number(place.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return;

    const category = place.subCategory || place.category || "부산 여행";
    result.push({
        id: `${place.name}-${place.rank ?? index + 1}-${latitude.toFixed(5)}`,
        name: place.name || `추천 장소 ${index + 1}`,
        district: place.district || "부산광역시",
        category,
        subCategory: place.subCategory,
        description: `${place.district || "부산"}에서 만나는 ${category} 여행지입니다.`,
        image: IMAGES[index % IMAGES.length],
        latitude,
        longitude,
        rank: place.rank ?? undefined,
    });
  });

  return result;
}

function formatDistance(meters: number): string {
  return meters < 1000 ? `${Math.round(meters)}m` : `${(meters / 1000).toFixed(1)}km`;
}

function formatDuration(milliseconds: number): string {
  const minutes = Math.max(1, Math.round(milliseconds / 60_000));
  const hours = Math.floor(minutes / 60);
  return hours ? `${hours}시간 ${minutes % 60}분` : `${minutes}분`;
}

function phaseLabel(phase: TravelPhase): string {
  return {
    idle: "여행을 시작할 준비가 되었어요",
    results: "추천 장소를 골라보세요",
    optimizing: "가장 효율적인 순서를 계산 중이에요",
    "route-ready": "오늘의 여행 경로가 준비됐어요",
    travelling: "ROAMATE가 여행을 함께하고 있어요",
    completed: "오늘의 여행을 잘 마무리했어요",
  }[phase];
}

export default function App() {
  const [location, setLocation] = useState<Coordinate>(BUSAN_CITY_HALL);
  const [accuracy, setAccuracy] = useState<number>();
  const [query, setQuery] = useState("");
  const [places, setPlaces] = useState<TourismPlace[]>([]);
  const [route, setRoute] = useState<OptimizedRoute>();
  const [selectedId, setSelectedId] = useState<string>();
  const [phase, setPhase] = useState<TravelPhase>("idle");
  const [notice, setNotice] = useState("부산의 테마를 검색하면 현재 위치에서 시작하는 여행 동선을 제안해 드려요.");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [completedCount, setCompletedCount] = useState(0);

  const orderedPlaces = route?.orderedPlaces ?? places;
  const selectedPlace = useMemo(
    () => orderedPlaces.find((place) => place.id === selectedId) ?? orderedPlaces[0],
    [orderedPlaces, selectedId],
  );
  const nextPlace = orderedPlaces[completedCount];

  const selectPlace = useCallback((place: TourismPlace) => {
    setSelectedId(place.id);
    setNotice(`${place.name} 정보를 확인하고 있어요.`);
  }, []);

  const locate = () => {
    if (!navigator.geolocation) {
      setNotice("이 브라우저에서는 위치를 가져올 수 없어 부산시청을 출발지로 사용합니다.");
      return;
    }

    setNotice("현재 위치를 확인하고 있어요.");
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({ latitude: position.coords.latitude, longitude: position.coords.longitude });
        setAccuracy(position.coords.accuracy);
        setNotice("현재 위치를 출발지로 설정했어요.");
      },
      () => setNotice("위치 권한을 받지 못해 부산시청을 출발지로 사용합니다."),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
    );
  };

  const runSearch = async (keyword: string) => {
    const normalizedKeyword = keyword.trim();
    if (!normalizedKeyword || loading) return;

    setLoading(true);
    setError("");
    setRoute(undefined);
    setCompletedCount(0);
    setPhase("idle");
    setNotice(`“${normalizedKeyword}”와 어울리는 부산 여행지를 찾고 있어요.`);

    try {
      const response = await searchTourism(normalizedKeyword);
      const results = toPlaces(response);
      setPlaces(results);
      setSelectedId(results[0]?.id);

      if (results.length) {
        setPhase("results");
        setNotice(
          response.source === "demo"
            ? `발표용 데모 코스 ${results.length}곳을 준비했어요. 실제 API 키를 설정하면 실시간 데이터로 바뀝니다.`
            : `${results.length}곳을 찾았어요. 마음에 드는 장소를 고르거나 바로 경로를 만들어 보세요.`,
        );
      } else {
        setPhase("idle");
        setNotice("지도에 표시할 수 있는 장소를 찾지 못했어요. 다른 테마를 검색해 보세요.");
      }
    } catch (caughtError) {
      setPlaces([]);
      setSelectedId(undefined);
      setPhase("idle");
      setError(caughtError instanceof Error ? caughtError.message : "관광지 검색 중 오류가 발생했습니다.");
      setNotice("관광지 정보를 불러오지 못했어요.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void runSearch(query);
  };

  const buildRoute = async () => {
    if (!places.length || loading) return;
    setLoading(true);
    setError("");
    setPhase("optimizing");
    setNotice("거리와 방문 순서를 비교해 가장 효율적인 경로를 계산하고 있어요.");

    try {
      const optimizedRoute = await optimizeRoute(location, places);
      setRoute(optimizedRoute);
      setSelectedId(optimizedRoute.orderedPlaces[0]?.id);
      setCompletedCount(0);
      setPhase("route-ready");
      setNotice(
        optimizedRoute.fallback
          ? "도로 경로 API가 설정되지 않아 직선거리 기반의 발표용 경로를 만들었어요."
          : "실제 도로 데이터를 반영한 최적 경로를 만들었어요.",
      );
    } catch (caughtError) {
      setPhase("results");
      setError(caughtError instanceof Error ? caughtError.message : "경로 계산 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const advanceTravel = () => {
    if (!route) {
      void buildRoute();
      return;
    }

    if (phase !== "travelling") {
      setPhase("travelling");
      setSelectedId(nextPlace?.id);
      setNotice(`${nextPlace?.name ?? "첫 번째 장소"}로 출발해 볼까요?`);
      return;
    }

    const nextCount = completedCount + 1;
    if (nextCount >= orderedPlaces.length) {
      setCompletedCount(nextCount);
      setPhase("completed");
      setNotice("모든 장소를 방문했어요. 오늘의 부산 여행은 어땠나요?");
      return;
    }

    setCompletedCount(nextCount);
    setSelectedId(orderedPlaces[nextCount].id);
    setNotice(`${orderedPlaces[nextCount].name}로 다음 일정을 이어가요.`);
  };

  const actionLabel = !route ? "최적 경로 만들기" : phase === "travelling" ? "다음 장소로" : phase === "completed" ? "새 코스 찾기" : "여행 시작하기";

  return (
    <div className="page">
      <section className="dashboard">
        <aside className="brand-panel">
          <div className="brand">
            <span aria-hidden="true">✦</span>
            <div>
              <strong>ROAMATE</strong>
              <small>AI REAL-TIME TRAVEL MATE</small>
            </div>
          </div>

          <h1>
            여행의 변수는 먼저 읽고,
            <em> 지금 필요한 다음 장소를 제안합니다.</em>
          </h1>
          <p>부산 관광 데이터, 현재 위치, 경로와 AI 대화를 하나의 여행 화면에서 연결합니다.</p>

          <div className="feature-list">
            {[
              ["◎", "현재 위치 기반", "출발지를 갱신해 동선을 다시 계산합니다."],
              ["↗", "스마트 루트", "방문 순서를 정리하고 길찾기 정보를 보여줍니다."],
              ["✦", "AI 여행 메이트", "날씨·시간·취향 변화에 맞춰 일정을 조정합니다."],
            ].map(([icon, title, description]) => (
              <article className="feature" key={title}>
                <span>{icon}</span>
                <div><strong>{title}</strong><p>{description}</p></div>
              </article>
            ))}
          </div>

          <button type="button" className="location" onClick={locate}>
            <i>◎</i>
            <span><strong>현재 위치 연결</strong><small>GPS를 출발지로 사용합니다</small></span>
          </button>
        </aside>

        <main className="console">
          <header className="search">
            <form onSubmit={handleSearch}>
              <span aria-hidden="true">⌕</span>
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="예: 해운대, 야경, 조용한 카페" disabled={loading} />
              <button type="submit" disabled={loading || !query.trim()}>{loading ? "찾는 중" : "검색"}</button>
            </form>
            <nav aria-label="추천 테마">
              {CATEGORIES.map((category) => (
                <button key={category} type="button" disabled={loading} onClick={() => { setQuery(category); void runSearch(category); }}>{category}</button>
              ))}
            </nav>
          </header>

          <div className="viewport">
            <RoamateMap currentLocation={location} accuracy={accuracy} places={places} selectedId={selectedPlace?.id} route={route} onSelect={selectPlace} />

            {selectedPlace && (
              <button type="button" className="place-card" onClick={() => selectPlace(selectedPlace)}>
                <img src={selectedPlace.image} alt="" />
                <span><small>{route ? `STOP ${Math.max(1, orderedPlaces.findIndex((place) => place.id === selectedPlace.id) + 1)}` : "RECOMMENDED"}</small><strong>{selectedPlace.name}</strong><b>{selectedPlace.category} · {selectedPlace.district}</b></span>
              </button>
            )}

            <section className="action" aria-live="polite">
              <span className="core">✦</span>
              <div><strong>{phaseLabel(phase)}</strong><p>{notice}</p></div>
              <button type="button" onClick={phase === "completed" ? () => void runSearch(query || "추천") : advanceTravel} disabled={loading || (!route && !places.length)}>{actionLabel}</button>
            </section>

            <section className="dock">
              <header>
                <strong>오늘의 여행 타임라인</strong>
                {route && <span>{formatDistance(route.totalDistanceMeters)} · {formatDuration(route.totalDurationMillis)}{route.fallback ? " · 예상 거리" : ""}</span>}
              </header>
              <div className="timeline">
                {orderedPlaces.length ? orderedPlaces.slice(0, 6).map((place, index) => (
                  <button type="button" className={selectedPlace?.id === place.id ? "selected" : ""} onClick={() => selectPlace(place)} key={place.id}>
                    <i>{index + 1}</i><img src={place.image} alt="" /><strong>{place.name}</strong><small>{place.category}</small>
                  </button>
                )) : <p>테마를 검색하면 여기에서 나만의 여행 코스를 만들 수 있어요.</p>}
              </div>
            </section>
          </div>

          {error && <div className="error" role="alert">{error}</div>}
          <ChatBottomSheet context={{ location, places: orderedPlaces, selectedPlace, route, phase, nextPlace }} />
        </main>

        <aside className="status">
          <article>
            <small>MOVE STATUS</small>
            <h2>{route ? route.guides[0]?.instruction ?? "다음 장소로 이동할 준비가 되었어요" : "경로를 만들면 이동 정보를 보여드려요"}</h2>
            <p>{route ? `${formatDistance(route.totalDistanceMeters)} · ${formatDuration(route.totalDurationMillis)}` : "출발지는 부산시청으로 설정되어 있습니다."}</p>
          </article>
          <article>
            <small>TRAVEL STATE</small>
            <h3>{phaseLabel(phase)}</h3>
            <p>{notice}</p>
            <button type="button" onClick={() => void buildRoute()} disabled={loading || !places.length}>경로 다시 계산</button>
          </article>
          <article>
            <small>OPTIMAL ORDER</small>
            {orderedPlaces.length ? orderedPlaces.slice(0, 6).map((place, index) => <button type="button" className={selectedPlace?.id === place.id ? "selected" : ""} onClick={() => selectPlace(place)} key={place.id}><i>{index + 1}</i>{place.name}</button>) : <p>검색 결과가 여기에 표시됩니다.</p>}
          </article>
        </aside>
      </section>
    </div>
  );
}

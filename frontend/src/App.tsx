import { useCallback, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { adjustItinerary, optimizeRoute, planItinerary, searchTourism } from "./api";
import ChatBottomSheet from "./components/ChatBottomSheet";
import RoamateMap from "./components/RoamateMap";
import type {
  Coordinate,
  ItineraryPlan,
  ItineraryRequest,
  OptimizedRoute,
  TourismPlace,
  TravelPhase,
} from "./types";
import "./styles.css";

const BUSAN_CITY_HALL: Coordinate = { latitude: 35.1795543, longitude: 129.0756416 };
const THEMES = ["바다", "야경", "카페", "문화", "시장", "체험", "산책"];
const IMAGES = [
  "https://images.unsplash.com/photo-1517154421773-0529f29ea451?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1588416936097-41850ab3d86d?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1000&q=80",
];

function localDate(): string {
  return formatDate(new Date());
}

function tomorrow(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return formatDate(date);
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function toPlaces(response: Awaited<ReturnType<typeof searchTourism>>): TourismPlace[] {
  return (response.places ?? []).flatMap((place, index) => {
    const latitude = Number(place.latitude);
    const longitude = Number(place.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return [];
    const category = place.subCategory || place.category || "부산 여행";
    return [{
      id: `${place.name}-${place.rank ?? index + 1}-${latitude.toFixed(5)}`,
      name: place.name || `추천 장소 ${index + 1}`,
      district: place.district || "부산광역시",
      category,
      subCategory: place.subCategory,
      description: `${place.district || "부산"}에서 만나는 ${category} 여행지예요.`,
      image: IMAGES[index % IMAGES.length],
      latitude,
      longitude,
      rank: place.rank ?? undefined,
    }];
  });
}

function formatDistance(meters: number): string {
  return meters < 1_000 ? `${Math.round(meters)}m` : `${(meters / 1_000).toFixed(1)}km`;
}

function formatDuration(milliseconds: number): string {
  const minutes = Math.max(1, Math.round(milliseconds / 60_000));
  const hours = Math.floor(minutes / 60);
  return hours ? `${hours}시간 ${minutes % 60}분` : `${minutes}분`;
}

function phaseLabel(phase: TravelPhase): string {
  return {
    idle: "여행 조건을 입력해 주세요",
    results: "장소를 골라 코스를 만들 수 있어요",
    optimizing: "방문 순서와 이동 경로를 계산 중이에요",
    "route-ready": "Gemma 여행 코스가 준비됐어요",
    travelling: "ROAMATE가 다음 목적지를 안내하고 있어요",
    completed: "오늘의 여행 코스를 마무리했어요",
  }[phase];
}

export default function App() {
  const [location, setLocation] = useState<Coordinate>(BUSAN_CITY_HALL);
  const [accuracy, setAccuracy] = useState<number>();
  const [planInput, setPlanInput] = useState<ItineraryRequest>({
    startDate: localDate(),
    endDate: tomorrow(),
    themes: ["바다", "야경"],
    companion: "친구와",
    transport: "대중교통",
    pace: "여유롭게",
    start: BUSAN_CITY_HALL,
  });
  const [itinerary, setItinerary] = useState<ItineraryPlan>();
  const [query, setQuery] = useState("");
  const [places, setPlaces] = useState<TourismPlace[]>([]);
  const [route, setRoute] = useState<OptimizedRoute>();
  const [selectedId, setSelectedId] = useState<string>();
  const [phase, setPhase] = useState<TravelPhase>("idle");
  const [notice, setNotice] = useState("여행 일자와 취향을 고르면 Gemma가 설명하고, ROAMATE가 검증된 동선으로 코스를 구성합니다.");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [completedCount, setCompletedCount] = useState(0);

  const orderedPlaces = itinerary?.orderedPlaces ?? route?.orderedPlaces ?? places;
  const selectedPlace = useMemo(
    () => orderedPlaces.find((place) => place.id === selectedId) ?? orderedPlaces[0],
    [orderedPlaces, selectedId],
  );
  const nextPlace = orderedPlaces[completedCount];

  const applyItinerary = useCallback((plan: ItineraryPlan, message?: string) => {
    setItinerary(plan);
    setPlaces(plan.orderedPlaces);
    setRoute(plan.route);
    setSelectedId(plan.orderedPlaces[0]?.id);
    setCompletedCount(0);
    setPhase("route-ready");
    setNotice(message ?? plan.summary);
  }, []);

  const selectPlace = useCallback((place: TourismPlace) => {
    setSelectedId(place.id);
    setNotice(`${place.name} 일정과 방문 이유를 확인하고 있어요.`);
  }, []);

  const toggleTheme = (theme: string) => {
    setPlanInput((current) => {
      const selected = current.themes.includes(theme)
        ? current.themes.filter((value) => value !== theme)
        : [...current.themes, theme].slice(0, 4);
      return { ...current, themes: selected.length ? selected : [theme] };
    });
  };

  const locate = () => {
    if (!navigator.geolocation) {
      setNotice("이 브라우저에서는 위치를 가져올 수 없어 부산시청을 출발지로 사용합니다.");
      return;
    }
    setNotice("현재 위치를 확인하고 있어요.");
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const next = { latitude: position.coords.latitude, longitude: position.coords.longitude };
        setLocation(next);
        setPlanInput((current) => ({ ...current, start: next }));
        setAccuracy(position.coords.accuracy);
        setNotice("현재 위치를 여행 출발지로 반영했어요.");
      },
      () => setNotice("위치 권한을 받지 못해 부산시청을 출발지로 사용합니다."),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
    );
  };

  const createPlan = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    if (loading) return;
    setLoading(true);
    setError("");
    setPhase("optimizing");
    setNotice("관광지 후보, 여행 변수, 이동 순서를 종합해 코스를 만들고 있어요.");
    try {
      const plan = await planItinerary({ ...planInput, start: location });
      applyItinerary(plan);
    } catch (caughtError) {
      setPhase("idle");
      setError(caughtError instanceof Error ? caughtError.message : "일정을 만들지 못했습니다.");
      setNotice("여행 조건을 다시 확인해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  const adjustPlan = async (adjustment: string): Promise<string | undefined> => {
    if (!itinerary) return undefined;
    setLoading(true);
    setError("");
    setNotice(`“${adjustment}” 요청을 반영해 일정과 경로를 다시 계산하고 있어요.`);
    try {
      const plan = await adjustItinerary({ ...planInput, start: location }, adjustment);
      applyItinerary(plan, `요청을 반영해 새 코스로 바꿨어요. ${plan.summary}`);
      return "일정을 다시 구성했어요. 지도와 타임라인에서 바뀐 순서를 확인해 보세요.";
    } catch (caughtError) {
      const message = caughtError instanceof Error ? caughtError.message : "일정을 조정하지 못했습니다.";
      setError(message);
      return undefined;
    } finally {
      setLoading(false);
    }
  };

  const runSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const keyword = query.trim();
    if (!keyword || loading) return;
    setLoading(true);
    setError("");
    setItinerary(undefined);
    setRoute(undefined);
    setCompletedCount(0);
    setPhase("idle");
    setNotice(`“${keyword}” 테마의 부산 관광지를 찾고 있어요.`);
    try {
      const results = toPlaces(await searchTourism(keyword));
      setPlaces(results);
      setSelectedId(results[0]?.id);
      setPhase(results.length ? "results" : "idle");
      setNotice(results.length ? `${results.length}곳을 찾았어요. 아래에서 바로 경로를 만들 수 있어요.` : "다른 테마로 다시 검색해 보세요.");
    } catch (caughtError) {
      setPlaces([]);
      setError(caughtError instanceof Error ? caughtError.message : "관광지 검색 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const buildRoute = async () => {
    if (!places.length || loading) return;
    setLoading(true);
    setPhase("optimizing");
    setNotice("가까운 방문 순서와 이동 경로를 계산하고 있어요.");
    try {
      const optimized = await optimizeRoute(location, places);
      setRoute(optimized);
      setSelectedId(optimized.orderedPlaces[0]?.id);
      setPhase("route-ready");
      setNotice(optimized.fallback ? "도로 길찾기 키가 없어 직선거리 기준의 발표용 경로를 표시합니다." : "실제 도로 정보를 반영한 경로예요.");
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
      setNotice("모든 장소를 방문했어요. ROAMATE AI에게 다음 여행 조건을 말해 보세요.");
      return;
    }
    setCompletedCount(nextCount);
    setSelectedId(orderedPlaces[nextCount].id);
    setNotice(`${orderedPlaces[nextCount].name}로 다음 일정을 이어가요.`);
  };

  const actionLabel = !route ? "경로 만들기" : phase === "travelling" ? "다음 장소로" : phase === "completed" ? "새 일정 만들기" : "여행 시작하기";

  return (
    <div className="page">
      <section className="dashboard">
        <aside className="brand-panel">
          <div className="brand"><span aria-hidden="true">✦</span><div><strong>ROAMATE</strong><small>GEMMA TRAVEL MATE</small></div></div>
          <h1>여행 변수를 먼저 읽고,<em> 지금 필요한 다음 장소를 제안합니다.</em></h1>
          <p>Gemma는 취향과 변경 요청을 이해하고, ROAMATE는 관광·날씨·교통 데이터와 검증된 경로로 일정의 뼈대를 만듭니다.</p>
          <div className="feature-list">
            {[["◎", "조건 기반 일정", "일자·동행·취향을 반영해 부산 코스를 구성합니다."], ["⌁", "실시간 컨텍스트", "날씨와 교통 데이터 상태를 일정과 함께 보여줍니다."], ["✦", "Gemma 여행 메이트", "비·피로·취향 변화에 맞춰 일정을 다시 조정합니다."]].map(([icon, title, description]) => (
              <article className="feature" key={title}><span>{icon}</span><div><strong>{title}</strong><p>{description}</p></div></article>
            ))}
          </div>
          <button type="button" className="location" onClick={locate}><i /><span><strong>현재 위치 연결</strong><small>GPS를 여행 출발지로 사용합니다.</small></span></button>
        </aside>

        <main className="console">
          <header className="planner">
            <div className="planner-heading"><div><small>AI ITINERARY BUILDER</small><strong>부산 여행을 설계해 볼까요?</strong></div><span>{itinerary?.source === "gemma" ? "Gemma 응답 연결" : "키 없이도 데모 생성 가능"}</span></div>
            <form onSubmit={(event) => void createPlan(event)}>
              <div className="planner-row dates"><label>시작<input type="date" value={planInput.startDate} onChange={(event) => setPlanInput((current) => ({ ...current, startDate: event.target.value, endDate: current.endDate < event.target.value ? event.target.value : current.endDate }))} disabled={loading} /></label><label>종료<input type="date" value={planInput.endDate} onChange={(event) => setPlanInput((current) => ({ ...current, endDate: event.target.value }))} disabled={loading} /></label></div>
              <div className="theme-chips" aria-label="여행 테마">{THEMES.map((theme) => <button className={planInput.themes.includes(theme) ? "selected" : ""} type="button" key={theme} onClick={() => toggleTheme(theme)} disabled={loading}>{theme}</button>)}</div>
              <div className="planner-row selects"><label>동행<select value={planInput.companion} onChange={(event) => setPlanInput((current) => ({ ...current, companion: event.target.value }))}><option>친구와</option><option>연인과</option><option>혼자</option><option>아이와</option><option>가족과</option></select></label><label>이동<select value={planInput.transport} onChange={(event) => setPlanInput((current) => ({ ...current, transport: event.target.value }))}><option>대중교통</option><option>자가용</option><option>도보 중심</option></select></label><label>속도<select value={planInput.pace} onChange={(event) => setPlanInput((current) => ({ ...current, pace: event.target.value }))}><option>여유롭게</option><option>알차게</option></select></label><button type="submit" disabled={loading}>{loading ? "설계 중" : "일정 만들기"}</button></div>
            </form>
            <form className="quick-search" onSubmit={(event) => void runSearch(event)}><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="또는 테마만 빠르게 검색: 야경, 시장, 카페" disabled={loading} /><button type="submit" disabled={loading || !query.trim()}>검색</button></form>
          </header>

          <div className="viewport">
            <RoamateMap currentLocation={location} accuracy={accuracy} places={orderedPlaces} selectedId={selectedPlace?.id} route={route} onSelect={selectPlace} />
            {itinerary && <section className="signal-strip"><span className={itinerary.context.weather.live ? "live" : ""}><b>날씨</b>{itinerary.context.weather.label}<small>{itinerary.context.weather.detail}</small></span><span className={itinerary.context.traffic.live ? "live" : ""}><b>교통</b>{itinerary.context.traffic.label}<small>{itinerary.context.traffic.detail}</small></span></section>}
            {selectedPlace && <button type="button" className="place-card" onClick={() => selectPlace(selectedPlace)}><img src={selectedPlace.image} alt="" /><span><small>{itinerary?.orderedPlaces.find((place) => place.id === selectedPlace.id)?.time || `STOP ${Math.max(1, orderedPlaces.findIndex((place) => place.id === selectedPlace.id) + 1)}`}</small><strong>{selectedPlace.name}</strong><b>{selectedPlace.category} · {selectedPlace.district}</b></span></button>}
            <section className="action" aria-live="polite"><span className="core">✦</span><div><strong>{phaseLabel(phase)}</strong><p>{notice}</p></div><button type="button" onClick={phase === "completed" ? () => void createPlan() : advanceTravel} disabled={loading || (!route && !places.length)}>{actionLabel}</button></section>
            <section className="dock"><header><strong>{itinerary ? itinerary.title : "오늘의 여행 타임라인"}</strong>{route && <span>{formatDistance(route.totalDistanceMeters)} · {formatDuration(route.totalDurationMillis)}{route.fallback ? " · 예상 거리" : ""}</span>}</header><div className="timeline">{orderedPlaces.length ? orderedPlaces.slice(0, 6).map((place, index) => <button type="button" className={selectedPlace?.id === place.id ? "selected" : ""} onClick={() => selectPlace(place)} key={place.id}><i>{index + 1}</i><img src={place.image} alt="" /><strong>{place.name}</strong><small>{itinerary?.orderedPlaces.find((stop) => stop.id === place.id)?.time || place.category}</small></button>) : <p>위 조건을 선택하면 여행 코스가 이곳에 완성됩니다.</p>}</div></section>
          </div>
          {error && <div className="error" role="alert">{error}</div>}
          <ChatBottomSheet context={{ location, itinerary, places: orderedPlaces, selectedPlace, route, phase, nextPlace }} onAdjustment={adjustPlan} />
        </main>

        <aside className="status">
          <article><small>TRAVEL SIGNAL</small><h2>{itinerary?.context.weather.label ?? "여행 데이터를 준비 중이에요"}</h2><p>{itinerary?.context.weather.detail ?? "일정을 만들면 날씨·교통 데이터 상태를 함께 보여드려요."}</p>{itinerary?.context.alerts.map((alert) => <p className="alert" key={alert}>• {alert}</p>)}</article>
          <article><small>AI SUMMARY</small><h3>{itinerary ? itinerary.summary : "조건을 선택하면 Gemma 기반 여행 요약을 만들어요."}</h3><p>{itinerary?.source === "gemma" ? "Gemma가 여행 조건과 데이터 컨텍스트를 바탕으로 요약했습니다." : "Gemma 키가 없어도 검증된 가이드 문장과 데모 데이터로 동작합니다."}</p><button type="button" onClick={() => void createPlan()} disabled={loading}>현재 조건으로 다시 설계</button></article>
          <article><small>DAY BY DAY</small>{itinerary?.days.length ? itinerary.days.map((day) => <div className="day-summary" key={day.day}><b>DAY {day.day}</b><span>{day.date}</span><small>{day.stops.map((stop) => `${stop.time} ${stop.name}`).join(" · ") || "자유 일정"}</small></div>) : <p>일정을 만들면 날짜별 방문 순서가 표시됩니다.</p>}</article>
        </aside>
      </section>
    </div>
  );
}

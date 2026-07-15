import {
 FormEvent,
 useCallback,
 useMemo,
 useState,
} from "react";

import { optimizeRoute, searchTourism } from "./api";
import ChatBottomSheet from "./components/ChatBottomSheet";
import RoamateMap from "./components/RoamateMap";

import type {
 Coordinate,
 OptimizedRoute,
 TourismPlace,
 TravelPhase,
} from "./types";

import "./styles.css";

const FALLBACK: Coordinate = {
 latitude: 35.1795543,
 longitude: 129.0756416,
};

const IMAGES = [
 "https://images.unsplash.com/photo-1517154421773-0529f29ea451?auto=format&fit=crop&w=1000&q=80",
 "https://images.unsplash.com/photo-1588416936097-41850ab3d86d?auto=format&fit=crop&w=1000&q=80",
 "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=1000&q=80",
];

/**
 * 화면에 표시하는 카테고리와
 * 실제 관광 API에 전달하는 검색어를 분리한다.
 */
const CATEGORY_KEYWORDS: Record<string, string> = {
 추천: "해운대",
 바다: "해수욕장",
 맛집: "시장",
 카페: "카페",
 실내: "박물관",
 야경: "전망대",
};

const CATEGORY_NAMES = Object.keys(CATEGORY_KEYWORDS);

/**
 * 다양한 외부 API 응답 구조에서 목록 배열을 찾는다.
 *
 * 현재 백엔드 내부 DTO:
 * {
 *   totalCount,
 *   pageNo,
 *   numOfRows,
 *   places: [...]
 * }
 */
function findItems(value: unknown): any[] {
 if (Array.isArray(value)) {
  return value;
 }

 if (!value || typeof value !== "object") {
  return [];
 }

 const objectValue = value as Record<string, unknown>;

 const candidateKeys = [
  "places",
  "item",
  "items",
  "results",
  "result",
  "data",
  "list",
  "body",
  "response",
 ];

 for (const key of candidateKeys) {
  const found = findItems(objectValue[key]);

  if (found.length > 0) {
   return found;
  }
 }

 for (const child of Object.values(objectValue)) {
  const found = findItems(child);

  if (found.length > 0) {
   return found;
  }
 }

 return [];
}

/**
 * 여러 후보 키 중 첫 번째 유효한 문자열 값을 반환한다.
 */
function getStringValue(
    object: any,
    keys: string[],
): string {
 for (const key of keys) {
  const value = object?.[key];

  if (
      typeof value === "string" &&
      value.trim()
  ) {
   return value.trim();
  }

  if (typeof value === "number") {
   return String(value);
  }
 }

 return "";
}

/**
 * 여러 후보 키 중 첫 번째 유효한 숫자 값을 반환한다.
 */
function getNumberValue(
    object: any,
    keys: string[],
): number {
 for (const key of keys) {
  const rawValue = object?.[key];

  if (
      rawValue === null ||
      rawValue === undefined ||
      rawValue === ""
  ) {
   continue;
  }

  const numberValue = Number(rawValue);

  if (Number.isFinite(numberValue)) {
   return numberValue;
  }
 }

 return Number.NaN;
}

/**
 * 외부 API 또는 내부 DTO 응답을 화면용 TourismPlace로 변환한다.
 */
function normalize(raw: unknown): TourismPlace[] {
 return findItems(raw)
     .map((item, index) => {
      const name =
          getStringValue(item, [
           "title",
           "name",
           "contenttitle",
           "hubTatsNm",
           "rlteTatsNm",
          ]) || `추천 장소 ${index + 1}`;

      const rank = getStringValue(item, [
       "rank",
       "hubRank",
       "rlteRank",
      ]);

      const district = getStringValue(item, [
       "district",
       "signguNm",
       "rlteSignguNm",
      ]);

      const category =
          getStringValue(item, [
           "subCategory",
           "category",
           "cat2",
           "hubCtgryMclsNm",
           "rlteCtgryMclsNm",
           "hubCtgryLclsNm",
           "rlteCtgryLclsNm",
          ]) || "관광지";

      return {
       id:
           getStringValue(item, [
            "contentid",
            "contentId",
            "id",
            "hubTatsCd",
            "rlteTatsCd",
           ]) ||
           `${name}-${rank || index + 1}`,

       name,

       description:
           getStringValue(item, [
            "overview",
            "description",
            "rlteTatsIntrcn",
           ]) ||
           "함께 방문하기 좋은 관광지입니다.",

       address:
           getStringValue(item, [
            "addr1",
            "address",
            "rlteBsicAdres",
           ]) ||
           district ||
           "부산광역시",

       image:
           getStringValue(item, [
            "firstimage",
            "firstImage",
            "image",
            "rlteTatsImg",
           ]) ||
           IMAGES[index % IMAGES.length],

       category,

       distance: getStringValue(item, [
        "distance",
        "rlteTatsDstnc",
       ]),

       latitude: getNumberValue(item, [
        "mapy",
        "mapY",
        "latitude",
        "lat",
        "rlteTatsYcrd",
       ]),

       longitude: getNumberValue(item, [
        "mapx",
        "mapX",
        "longitude",
        "lng",
        "rlteTatsXcrd",
       ]),
      };
     })
     .filter(
         (place) =>
             Number.isFinite(place.latitude) &&
             Number.isFinite(place.longitude),
     );
}

function formatDistance(meters: number): string {
 if (meters < 1000) {
  return `${Math.round(meters)}m`;
 }

 return `${(meters / 1000).toFixed(1)}km`;
}

function formatDuration(
    milliseconds: number,
): string {
 const minutes = Math.max(
     1,
     Math.round(milliseconds / 60000),
 );

 const hours = Math.floor(minutes / 60);
 const remainingMinutes = minutes % 60;

 if (hours > 0) {
  return `${hours}시간 ${remainingMinutes}분`;
 }

 return `${minutes}분`;
}

export default function App() {
 const [location, setLocation] =
     useState<Coordinate>(FALLBACK);

 const [accuracy, setAccuracy] =
     useState<number>();

 const [query, setQuery] = useState("");

 const [places, setPlaces] = useState<
     TourismPlace[]
 >([]);

 const [selectedId, setSelectedId] =
     useState<string>();

 const [route, setRoute] =
     useState<OptimizedRoute>();

 const [phase, setPhase] =
     useState<TravelPhase>("idle");

 const [notice, setNotice] = useState(
     "관광지를 검색하면 내 위치에서 출발하는 최적 경로를 만들 수 있어요.",
 );

 const [error, setError] = useState("");

 const [loading, setLoading] =
     useState(false);

 const ordered =
     route?.orderedPlaces ?? places;

 const selected = useMemo(
     () =>
         ordered.find(
             (place) => place.id === selectedId,
         ),
     [ordered, selectedId],
 );

 const select = useCallback(
     (place: TourismPlace) => {
      setSelectedId(place.id);
      setNotice(
          `${place.name}을 선택했습니다.`,
      );
     },
     [],
 );

 /**
  * 브라우저 현재 위치를 조회한다.
  */
 function locate() {
  if (!navigator.geolocation) {
   setNotice(
       "현재 브라우저는 위치 조회를 지원하지 않아 부산시청을 출발지로 사용합니다.",
   );
   return;
  }

  navigator.geolocation.getCurrentPosition(
      (position) => {
       setLocation({
        latitude:
        position.coords.latitude,
        longitude:
        position.coords.longitude,
       });

       setAccuracy(
           position.coords.accuracy,
       );

       setNotice(
           "현재 위치를 연결했습니다.",
       );
      },
      () => {
       setNotice(
           "위치 권한이 없어 부산시청을 임시 출발지로 사용합니다.",
       );
      },
      {
       enableHighAccuracy: true,
       timeout: 10000,
      },
  );
 }

 /**
  * 검색창 및 카테고리 버튼이 공통으로 사용하는 검색 함수.
  */
 async function executeSearch(
     keyword: string,
 ) {
  const trimmedKeyword = keyword.trim();

  if (!trimmedKeyword || loading) {
   return;
  }

  setLoading(true);
  setError("");
  setRoute(undefined);
  setPlaces([]);
  setSelectedId(undefined);
  setPhase("idle");

  try {
   const raw =
       await searchTourism(
           trimmedKeyword,
       );

   const list = normalize(raw);

   setPlaces(list);
   setSelectedId(list[0]?.id);

   if (list.length > 0) {
    setPhase("places-ready");

    setNotice(
        `${list.length}개 장소를 찾았습니다.`,
    );
   } else {
    setPhase("idle");

    setNotice(
        `"${trimmedKeyword}" 검색 결과가 없거나 지도에 표시할 좌표가 없습니다.`,
    );
   }
  } catch (caughtError) {
   setPlaces([]);
   setSelectedId(undefined);
   setPhase("idle");

   setError(
       caughtError instanceof Error
           ? caughtError.message
           : "관광지 검색 중 오류가 발생했습니다.",
   );

   setNotice(
       "관광지 정보를 불러오지 못했습니다.",
   );
  } finally {
   setLoading(false);
  }
 }

 /**
  * 검색창 제출 처리.
  */
 async function search(
     event: FormEvent<HTMLFormElement>,
 ) {
  event.preventDefault();
  await executeSearch(query);
 }

 /**
  * 카테고리 버튼 클릭 처리.
  *
  * 예:
  * 바다 → 해수욕장
  * 실내 → 박물관
  */
 async function handleCategoryClick(
     category: string,
 ) {
  const keyword =
      CATEGORY_KEYWORDS[category] ??
      category;

  setQuery(keyword);

  await executeSearch(keyword);
 }

 /**
  * 조회된 장소의 최적 경로를 계산한다.
  */
 async function build() {
  if (!places.length) {
   setError(
       "먼저 장소를 검색해주세요.",
   );
   return;
  }

  setError("");
  setPhase("route-loading");

  setNotice(
      "모든 장소의 방문 순서와 실제 도로 경로를 계산하고 있습니다.",
  );

  try {
   const result = await optimizeRoute(
       location,
       places,
   );

   setRoute(result);

   setSelectedId(
       result.orderedPlaces[0]?.id,
   );

   setPhase("route-ready");

   setNotice(
       `총 ${formatDistance(
           result.totalDistanceMeters,
       )} · ${formatDuration(
           result.totalDurationMillis,
       )} 경로를 만들었습니다.`,
   );
  } catch (caughtError) {
   setError(
       caughtError instanceof Error
           ? caughtError.message
           : "경로 계산 중 오류가 발생했습니다.",
   );

   setPhase("places-ready");
  }
 }

 return (
     <div className="page">
      <section className="dashboard">
       <aside className="brand-panel">
        <div className="brand">
         <span>✦</span>

         <div>
          <strong>ROAMATE</strong>
          <small>
           AI REAL-TIME TRAVEL OS
          </small>
         </div>
        </div>

        <h1>
         여행의 변화를 먼저 읽고,
         <em>
          {" "}
          지금 필요한 한 가지를
         </em>{" "}
         제안합니다.
        </h1>

        <p>
         현재 위치, 공공 관광데이터,
         실제 도로 경로와 AI 대화를 한
         화면에서 연결합니다.
        </p>

        {[
         [
          "◉",
          "실시간 위치 분석",
         ],
         ["⌁", "최적 경로 엔진"],
         ["✦", "Hidden Spot 추천"],
         [
          "⚡",
          "원터치 여행 액션",
         ],
        ].map(([icon, title]) => (
            <article
                className="feature"
                key={title}
            >
             <span>{icon}</span>

             <div>
              <strong>{title}</strong>

              <p>
               검색·경로·이동 상태에
               따라 유동적으로
               반응합니다.
              </p>
             </div>
            </article>
        ))}

        <button
            type="button"
            className="location"
            onClick={locate}
        >
         <i />

         <span>
              <strong>
                현재 위치 연결
              </strong>

              <small>
                눌러서 GPS를 다시
                확인하세요
              </small>
            </span>
        </button>
       </aside>

       <main className="console">
        <header className="search">
         <form onSubmit={search}>
          <span>⌕</span>

          <input
              value={query}
              onChange={(event) =>
                  setQuery(
                      event.target.value,
                  )
              }
              placeholder="어디로 떠날까요?"
              disabled={loading}
          />

          <button
              type="submit"
              disabled={
                  loading ||
                  !query.trim()
              }
          >
           {loading
               ? "SEARCHING"
               : "SEARCH"}
          </button>
         </form>

         <nav>
          {CATEGORY_NAMES.map(
              (category) => (
                  <button
                      type="button"
                      key={category}
                      disabled={loading}
                      onClick={() =>
                          handleCategoryClick(
                              category,
                          )
                      }
                  >
                   {category}
                  </button>
              ),
          )}
         </nav>
        </header>

        <div className="viewport">
         <RoamateMap
             currentLocation={location}
             accuracy={accuracy}
             places={places}
             selectedId={selectedId}
             route={route}
             onSelect={select}
         />

         {selected && (
             <button
                 type="button"
                 className="place-card"
                 onClick={() =>
                     select(selected)
                 }
             >
              <img
                  src={selected.image}
                  alt={selected.name}
              />

              <span>
                  <small>
                    NEXT PLACE
                  </small>

                  <strong>
                    {selected.name}
                  </strong>

                  <b>
                    {selected.category}
                  </b>
                </span>
             </button>
         )}

         <section className="action">
              <span className="core">
                ✦
              </span>

          <div>
           <strong>
            {phaseLabel(phase)}
           </strong>

           <p>{notice}</p>
          </div>

          {!route ? (
              <button
                  type="button"
                  onClick={build}
                  disabled={
                      loading ||
                      !places.length
                  }
              >
               최적 경로 만들기
              </button>
          ) : phase !== "moving" ? (
              <button
                  type="button"
                  onClick={() => {
                   setPhase("moving");

                   setNotice(
                       `${
                           ordered[0]?.name ??
                           "첫 번째 장소"
                       }으로 이동을 시작합니다.`,
                   );
                  }}
              >
               이동 시작
              </button>
          ) : (
              <button
                  type="button"
                  onClick={() => {
                   setPhase("arrived");

                   setNotice(
                       `${
                           selected?.name ??
                           "선택한 장소"
                       }에 도착했습니다.`,
                   );
                  }}
              >
               도착 처리
              </button>
          )}
         </section>

         <section className="dock">
          <header>
           <strong>
            오늘의 최적 일정
           </strong>

           {route && (
               <span>
                    {formatDistance(
                        route.totalDistanceMeters,
                    )}{" "}
                ·{" "}
                {formatDuration(
                    route.totalDurationMillis,
                )}
                  </span>
           )}
          </header>

          <div className="timeline">
           {ordered.length > 0 ? (
               ordered
                   .slice(0, 5)
                   .map(
                       (
                           place,
                           index,
                       ) => (
                           <button
                               type="button"
                               className={
                                selectedId ===
                                place.id
                                    ? "selected"
                                    : ""
                               }
                               onClick={() =>
                                   select(place)
                               }
                               key={place.id}
                           >
                            <i>
                             {index + 1}
                            </i>

                            <img
                                src={
                                 place.image
                                }
                                alt={
                                 place.name
                                }
                            />

                            <strong>
                             {place.name}
                            </strong>

                            <small>
                             {
                              place.category
                             }
                            </small>
                           </button>
                       ),
                   )
           ) : (
               <p>
                장소를 검색하면
                디지털 타임라인이
                생성됩니다.
               </p>
           )}
          </div>
         </section>
        </div>

        {error && (
            <div className="error">
             {error}
            </div>
        )}

        <ChatBottomSheet
            context={{
             currentLocation:
             location,
             selectedPlace:
             selected,
             route,
             places: ordered,
            }}
        />
       </main>

       <aside className="status">
        <article>
         <small>MOVE STATUS</small>

         <h2>
          {route
              ? phase === "moving"
                  ? route.guides[0]
                      ?.instruction ||
                  "경로를 따라 이동 중"
                  : "경로 준비 완료"
              : "경로 대기"}
         </h2>

         <p>
          {route
              ? `${formatDistance(
                  route.totalDistanceMeters,
              )} · ${formatDuration(
                  route.totalDurationMillis,
              )}`
              : "최적 경로를 만들면 이동 정보가 표시됩니다."}
         </p>
        </article>

        <article>
         <small>TRAVEL STATE</small>

         <h3>
          {phaseLabel(phase)}
         </h3>

         <p>{notice}</p>

         <button
             type="button"
             onClick={build}
             disabled={
                 loading ||
                 !places.length
             }
         >
          경로 다시 계산
         </button>
        </article>

        <article>
         <small>
          OPTIMAL ORDER
         </small>

         {ordered
             .slice(0, 5)
             .map(
                 (place, index) => (
                     <button
                         type="button"
                         className={
                          selectedId ===
                          place.id
                              ? "selected"
                              : ""
                         }
                         onClick={() =>
                             select(place)
                         }
                         key={place.id}
                     >
                      <i>{index + 1}</i>
                      {place.name}
                     </button>
                 ),
             )}
        </article>
       </aside>
      </section>
     </div>
 );
}

function phaseLabel(
    phase: TravelPhase,
): string {
 return {
  idle: "새로운 여행을 시작할 준비가 됐어요",
  "places-ready":
      "추천 장소를 경로로 연결할 수 있어요",
  "route-loading":
      "모든 장소의 최적 순서를 계산하고 있어요",
  "route-ready":
      "실제 도로 기반 최적 경로가 준비됐어요",
  moving:
      "다음 장소로 이동 중이에요",
  arrived:
      "장소에 도착했어요",
 }[phase];
}
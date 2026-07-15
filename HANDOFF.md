# HANDOFF — 2026-07-15 (BE-3 작업 인계)

> 오늘 하루 동안 main이 한 번 완전히 깨졌다가 복구됐고, AI 채팅을 Gemma로 실연동했고,
> 관광지 검색 500 버그를 고쳤고, 마지막으로 검색창의 좌표 버그를 발견(미수정)한 상태.
> 아래 순서대로 읽으면 오늘 무슨 일이 있었는지 파악 가능.

## 1. 시작 시점 상태 — main이 컴파일 자체가 안 됐음

`raks030517`이 `버전 1.0.0 통합`(`3a06d86`) 커밋으로 오래된 로컬 브랜치(nari의 관광지 작업 이전
시점에서 갈라진)를 main에 합치면서 충돌 처리를 잘못함:
- `ChatRequest`/`ChatResponse`는 삭제했는데, 그걸 쓰는 `ChatController`/`TravelChatService`는 안 지움 → **컴파일 실패**.
- `CorsConfig`/`WebClientConfig` 삭제됨 → 살아있었어도 CORS 전부 막히고 `WebClient.Builder` 빈이 없어서 모든 API 클라이언트가 기동 자체를 못 했을 상태.
- `BusanTripApplication`에서 `@ConfigurationPropertiesScan` 빠짐 → `ApiKeyProperties` 빈 자체가 안 생성됨.
- `nari`가 `TourismController`(`/api/tourism`)를 `PlaceController`(`/api/places`)로 리네임했는데, 이미 배포된 프론트 3곳(`api.ts`/`systemApi.ts`/`tourismApi.ts`)이 옛 경로(`/api/tourism/...`)를 하드코딩 중이라 프론트가 깨져있었음.

이 상태로는 아무도 `main`을 pull받아 정상 작동시킬 수 없었음.

## 2. 오늘 한 작업 (커밋 순서대로)

### `002b0c2` — junho의 여행검색 기능 채택
Chris 판단: 내(BE-3)가 만들었던 Gemini 기반 `TravelChatService`(`/api/chat`)를 버리고,
junho가 독립적으로 만든 더 완성도 높은 파이프라인을 채택.
- `LlmQueryService`(Gemma) → `SearchCondition`(의도 분류: 관광/음식점/코스) → `TravelSearchService`
  (병렬검색+중복제거+부분실패 처리) → `POST /api/travel/search`
- `PlaceController`(`GET /api/places/search`, 네이버 원본 검색), `LlmController`(디버그용),
  AVI 교통량(`AviTrafficClient`/`Service`/`TrafficController`)도 함께 채택.
- `PlaceResponse` 이름 충돌(관광공사 API용 vs 네이버 원본용)은 후자를 `NaverPlaceResponse`로
  분리해서 해소.
- **주의**: `/api/travel/search`, `/api/places/search`는 아직 **어느 프론트 화면에서도 안 부름**.
  API로는 살아있고 테스트도 통과하지만 UI에 연결 안 된 상태.

### `b5a1112` — main 빌드 복구 + Gemma 채팅 실연동
1번의 main 붕괴를 정리하고, `AiChatController`(`/api/ai/chat` — raks030517이 만든 대화형 AI
비서, 프론트 `ChatBottomSheet`가 실제로 부르는 엔드포인트)의 내부 구현을 Gemini에서 Gemma로
교체. **프론트는 무변경** — `AiChatController`의 요청/응답 계약은 그대로 두고, 내부에서
junho의 `GemmaClient`를 재사용하도록 `GemmaChatService`로 갈아끼움.

발견/수정한 것:
- Gemma 모델명이 실제 존재하지 않는 값(`gemma-3-27b-it`, 404)이었음 → `ListModels`로 실제
  사용 가능한 모델 확인 후 `gemma-4-31b-it`로 교체.
- `gemma-4-31b-it`는 응답에 `thought=true`인 추론 파트를 최종답변 앞에 먼저 보내는데,
  `GemmaClient`가 이걸 안 걸러내고 그대로 이어붙여서 사용자 답변에 초안·체크리스트가
  섞여 나갔음 → `thought=true` 파트 필터링하도록 수정 + 회귀테스트 추가.
- `nari`의 `PlaceController` 리네임을 `TourismController`(`/api/tourism`)로 원복 —
  이미 배포된 프론트가 옛 경로를 쓰고 있어서 프론트 코드를 안 건드리는 쪽으로 백엔드를 맞춤.

### `d7a3dd0` — 관광지 검색 결과 0건일 때 500 나던 버그
Chris가 실제로 재현: 검색 결과 없는 키워드로 `/api/tourism/related/search` 호출 시 500.
- `GlobalExceptionHandler`(junho 버전 채택 과정에서)가 예외를 로그 없이 삼키고 있어서
  원인 파악 자체가 안 됐음 → 로그 복원.
- 원인 ①: 공공데이터포털 API가 결과 0건일 때 `items` 필드를 빈 문자열(`""`)로 내려주는데
  Jackson 3가 이걸 객체로 코어션 못 해서 예외 → `WebClientConfig`에 `EmptyString→null`
  코어션 설정 추가.
- 원인 ②: 그 다음 `items`가 정상적으로 `null`이 되니, `TourismService`가 null 체크 없이
  `.item()`을 바로 불러서 NPE → null이면 빈 리스트로 처리하도록 수정.
- 검증: 전체 테스트 31/31, 정상/0건 키워드 둘 다 실제 curl로 확인.

## 3. 지금 상태 (2026-07-15 기준 main)

| 기능 | 상태 |
|---|---|
| 지도(`RoamateMap`) + 경로최적화(`/api/routes/optimize`) | 정상 |
| AI 채팅(`ChatBottomSheet` → `/api/ai/chat`) | 정상, **Gemma**로 실제 응답 확인함 |
| 관광지 검색(검색창 → `/api/tourism/related/search`) | API는 정상(500 버그 고침), **근데 화면에서 결과가 사실상 안 보이는 문제 있음 — 4번 참고, 미해결** |
| junho의 구조화 검색(`/api/travel/search`) | API 정상, 테스트 통과, **프론트 미연결** |
| AVI 교통량(`/api/traffic/avi`) | API만 존재, 프론트 미연결 |

## 4. 미해결 — 검색창에 키워드 넣으면 사실상 응답 없어 보이는 문제

Chris가 보고: "키워드와 검색란에 직접 작성하면 응답이 없어." 원인 진단은 끝났고 **아직 안 고침**:

1. **근본 원인**: `/api/tourism/related/search`(연관 관광지 검색)는 관광공사 API 자체가
   좌표(위도/경도)를 안 줌 — `TourismService.toRelatedTourismResponse()`에서 `latitude`/
   `longitude`가 항상 `null`로 나가는 게 맞는 동작임(데이터가 없음).
2. **프론트 버그(부수적, 증상을 더 헷갈리게 만듦)**: `App.tsx`의 `normalize()` 안 `n()` 함수가
   `Number(null)`을 계산하는데, 자바스크립트에서 `Number(null) === 0`이라 **유효한 숫자로
   오판**함. 그래서 "좌표 없는 결과는 걸러내자"는 필터가 작동을 안 하고, 검색은 "15개
   찾았다"고 뜨는데 전부 좌표가 (0,0)(아프리카 서해안)이라 지도엔 아무것도 안 보임.
   → 이건 지금 당장 고쳐도 되는 안전한 버그(정직하게 "결과 없음"으로 바뀔 뿐).
3. **진짜 문제는 2번을 고쳐도 안 풀림**: 애초에 이 API가 좌표를 안 주니까, 버그를 고치면
   오히려 검색 결과가 **항상 0개**로 보이게 됨(더 정직해지지만 여전히 못 씀).
   **팀 결정 필요** — 옵션:
   - (a) 검색 결과 이름을 다시 네이버 지역검색(`PlaceController`, 이미 있음)에 태워서 좌표 보완
   - (b) 검색창을 좌표 있는 다른 엔드포인트(`/api/tourism/local` — 키워드 없이 지역 전체 조회, 좌표 있음)로 바꾸기
   - (c) 그 외

## 5. 팀원들에게 알려야 할 것 (Chris가 직접 전달 필요)

- **raks030517**: `버전 1.0.0 통합` 병합이 main을 컴파일 자체가 안 되는 상태로 만들었었음(1번
  참고). 지금은 고쳐서 push해뒀지만, 오래된 로컬 브랜치를 합칠 땐 병합 전에 `git fetch`+최신
  main 기준으로 rebase하거나 최소한 병합 후 빌드 확인이 필요함.
- **nari**: `PlaceController` 리네임(`/api/tourism`→`/api/places`)을 원복함 — 프론트 3곳이
  옛 경로를 하드코딩하고 있어서 리네임이 프론트를 깨뜨리고 있었음. 다시 리네임하고 싶으면
  프론트 쪽 경로도 같이 바꿔야 함.
- **junho**: `TravelSearchService`/`/api/travel/search`는 살아있고 테스트도 통과하는데
  프론트 어디서도 안 부름 — 4번의 검색창 문제 해결책으로 쓰일 수 있어 보임(옵션 a).

## 6. 다음 할 일 제안

1. 4번 검색 좌표 문제 — 팀 결정 후 수정 (우선순위 높음, 사용자 대면 핵심 기능)
2. junho의 `/api/travel/search`, AVI 교통량 기능을 프론트에 실제로 연결할지 결정
3. `RouteOptimizationService`가 `application-roamate.yml`의 네이버 Directions 키를 못 읽는
   상태(프로필이 활성화 안 돼있어서 직선 fallback만 동작) — 필요하면 profile 활성화 또는
   메인 `application.yml`로 이동

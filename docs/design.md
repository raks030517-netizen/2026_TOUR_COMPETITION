# 설계·계약 문서 (Day 2 산출물) — 팀 부산트립

> 이 문서는 프론트엔드와 백엔드의 단일 계약 원본(Single Source of Truth)이다. 구현 중 필드명, 타입, HTTP 상태 코드 또는 오류 JSON을 바꾸려면 양쪽 담당자가 이 문서를 먼저 함께 수정한다.

## 0. 메타

| 항목 | 내용 |
| --- | --- |
| 팀 | 부산트립 |
| 작성일 | 2026-07-13 |
| 관련 Issue / PR | 미정 — Issue 생성 후 번호 기입 |
| 스택 | Java 21 · Spring Boot 4.1.x · Spring AI 2.0.0 · Gemini(`gemini-3.1-flash-lite`) · React(Vite) · base 패키지 `com.study` |

## 1. 문제·사용자 (한 문장)

- 사용자: 부산을 처음 방문해 지역과 동선을 잘 모르는 1~2인 자유여행객
- 문제: 자신의 취향과 현재 위치에 맞는 장소를 찾기 위해 블로그, 지도, 날씨 정보를 반복해서 검색하고 비교해야 한다.
- 한 문장 정의: **“우리 팀은 부산을 처음 방문한 1~2인 자유여행객을 위해 여러 서비스에서 장소와 날씨를 반복 검색하는 불편을, AI가 자연어 요청을 검색 조건으로 바꾸고 맞춤 장소와 설명을 한 번에 제시하는 방식으로 풀어 준다.”**

## 2. 기능 범위 — MoSCoW

| 구분 | 기능 |
| --- | --- |
| Must (절대 사수) | ① 자연어 여행 요청 입력 ② Gemini를 이용한 의도 분석 및 구조화 응답 생성 ③ 네이버 지역 검색을 통한 실제 장소 조회 ④ 추천 장소 목록과 지도 마커 표시 ⑤ 추천 결과 DB 저장 및 단건 재조회 |
| Should (시간 허락 시) | 기상청 예보를 조회해 날씨에 맞는 실내·실외 장소 추천 |
| Could (과감히 포기) | 사용자 로그인, 일정 자동 최적화, 다중 사용자 대화 기억, 이미지 입력, 실시간 스트리밍 |

**오늘(Day 2) 코드로 증명할 1개:** 사용자의 `message`를 Gemini에 보내고 `TravelRecommendationResult`로 구조화해 받는 호출 1회

## 3. 핵심 시나리오 (MVP 한 흐름)

| 구분 | 흐름 |
| --- | --- |
| 입력 (User) | 사용자가 “해운대 근처에서 혼자 가기 좋은 일식집을 알려줘”를 입력하고 추천 버튼을 클릭한다. |
| 전송 (System) | React가 백엔드 `POST /api/chat`으로 `{ "message": "..." }`를 전송한다. |
| 처리 (System) | Spring AI가 요청을 추천 설명과 지역 검색어로 구조화하고, Service가 검색어로 네이버 지역 검색 API를 호출해 장소를 조회한다. |
| 저장 (System) | 원문 요청, AI 추천 설명, 최종 장소 목록 JSON, 생성 시각을 `travel_recommendations` 테이블에 저장한다. |
| 출력 (System) | 추천 설명과 장소 카드가 표시되고, 각 장소의 위도·경도로 네이버 지도 마커가 표시된다. |

## 4. 아키텍처 · 책임 분리 (Separation of Concerns)

```text
React (Vite) ──HTTP──> Spring Boot (com.study) ──> Spring AI ──> Gemini
      │                     │          │
      │                     │          ├──> 네이버 지역 검색 API
      │                     │          └──> 기상청 API (Should)
      │                     └──> DB (travel_recommendations)
      └──> 네이버 지도 JavaScript API
```

| 레이어 | 책임 |
| --- | --- |
| React | 사용자 입력 수신, API 요청, 로딩·성공·오류 상태 관리, 장소 카드와 지도 마커 표시 |
| Controller | HTTP 요청 수신, Bean Validation, Service 호출, 계약된 상태 코드 반환 |
| Service | 프롬프트 구성, AI 호출, 검색어로 외부 장소 조회, 응답 가공, 트랜잭션 단위 저장 지시 |
| Spring AI | `ChatClient`로 Gemini 통신, `.entity(TravelRecommendationResult.class)`로 구조화 출력 변환 |
| External API Client | 네이버 지역 검색 및 기상청 API 통신, 외부 응답을 내부 타입으로 변환 |
| Repository | `travel_recommendations` 단건 저장·조회 |

## 5. API 계약 ⭐

### 5.1 공통 규칙

- 요청과 응답의 `Content-Type`은 `application/json`이다.
- JSON 필드명은 **camelCase**를 사용한다.
- 시각은 서버의 Asia/Seoul 기준 `LocalDateTime`을 ISO-8601 문자열(`yyyy-MM-dd'T'HH:mm:ss`)로 반환한다.
- 오류 응답은 모든 엔드포인트에서 `{ "message": "string" }` 한 형태만 사용한다.
- 장소를 찾지 못한 경우는 실패가 아니다. `200 OK`와 빈 `places: []`를 반환하며 `message`로 다른 조건 입력을 안내한다.

### 5.2 엔드포인트 목록

| Method | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/chat` | 자연어 요청을 분석해 부산 장소를 추천하고 결과를 저장한다. |
| `GET` | `/api/recommendations/{recommendationId}` | 저장된 추천 결과 한 건을 다시 조회한다. |
| `GET` | `/api/system/health` | 백엔드 동작 상태를 확인한다. |
| `GET` | `/api/system/config-status` | 외부 API 설정 여부만 확인한다. 키 값 자체는 반환하지 않는다. |

### 5.3 상세 — `POST /api/chat`

#### 요청 (Request Body)

```json
{
  "message": "해운대 근처에서 혼자 가기 좋은 일식집을 알려줘"
}
```

| 필드 | 타입 | 필수 | 제약·설명 |
| --- | --- | --- | --- |
| `message` | `string` | ✅ | 공백 제거 후 1~500자, 사용자의 자연어 여행 요청 |

백엔드 DTO:

```java
public record ChatRequest(
    @NotBlank(message = "여행 요청을 입력해 주세요.")
    @Size(max = 500, message = "여행 요청은 500자 이하로 입력해 주세요.")
    String message
) {}
```

#### 응답 — 성공 `200 OK`

```json
{
  "recommendationId": 1,
  "message": "혼자 식사하기 편하고 해운대에서 이동하기 좋은 일식 장소를 골랐어요.",
  "places": [
    {
      "name": "해운대 ○○스시",
      "address": "부산광역시 해운대구 ○○로 10",
      "latitude": 35.1631,
      "longitude": 129.1635
    }
  ],
  "createdAt": "2026-07-13T15:30:00"
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `recommendationId` | `number` (정수, Java `Long`) | 저장된 추천 결과 식별자 |
| `message` | `string` | 사용자에게 보여 줄 AI 추천 설명 |
| `places` | `Place[]` | 추천 장소 목록, 결과가 없으면 빈 배열 |
| `places[].name` | `string` | HTML 태그를 제거한 장소명 |
| `places[].address` | `string` | 도로명 주소 우선, 없으면 지번 주소 |
| `places[].latitude` | `number` (Java `Double`) | WGS84 위도 |
| `places[].longitude` | `number` (Java `Double`) | WGS84 경도 |
| `createdAt` | `string` | ISO-8601 생성 시각 |

백엔드·프론트 타입:

```java
public record ChatResponse(
    Long recommendationId,
    String message,
    List<PlaceResponse> places,
    LocalDateTime createdAt
) {}

public record PlaceResponse(
    String name,
    String address,
    Double latitude,
    Double longitude
) {}
```

```ts
export interface Place {
  name: string
  address: string
  latitude: number
  longitude: number
}

export interface ChatResponse {
  recommendationId: number
  message: string
  places: Place[]
  createdAt: string
}
```

#### 응답 — 오류

| 상태 코드 | 상황 | 응답 예시 |
| --- | --- | --- |
| `400 Bad Request` | `message` 누락·공백 | `{ "message": "여행 요청을 입력해 주세요." }` |
| `400 Bad Request` | `message`가 500자 초과 | `{ "message": "여행 요청은 500자 이하로 입력해 주세요." }` |
| `500 Internal Server Error` | Gemini·네이버 API 호출 실패 또는 응답 변환 실패 | `{ "message": "추천을 만들지 못했습니다. 잠시 후 다시 시도해 주세요." }` |
| `500 Internal Server Error` | DB 저장 실패 | `{ "message": "추천 결과를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요." }` |

외부 API 호출 또는 DB 저장이 실패하면 추천 레코드를 남기지 않는다. 프론트는 `response.ok`가 거짓일 때 위 JSON의 `message`를 오류 UI에 표시하고, 알 수 없는 형식일 때만 “요청 처리 중 오류가 발생했습니다.”를 표시한다.

### 5.4 상세 — `GET /api/recommendations/{recommendationId}`

- Path variable `recommendationId`: 1 이상의 정수(Java `Long`)
- 성공: `200 OK`, 응답 본문은 `POST /api/chat` 성공 응답과 동일
- 없는 ID: `404 Not Found`, `{ "message": "추천 결과를 찾을 수 없습니다." }`
- 형식이 잘못된 ID: `400 Bad Request`, `{ "message": "추천 결과 ID가 올바르지 않습니다." }`

### 5.5 기존 시스템 API 계약

```json
// GET /api/system/health — 200 OK
{ "status": "UP" }
```

```json
// GET /api/system/config-status — 200 OK
{
  "naverSearchConfigured": true,
  "geminiConfigured": true,
  "weatherConfigured": false
}
```

## 6. 데이터 모델 (DB 스키마)

단일 테이블 `travel_recommendations`만 사용한다. Java Entity 필드명은 camelCase, 실제 DB 컬럼명은 snake_case로 고정한다.

| Java 필드 / DB 컬럼 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` / `id` | `Long` / `BIGINT` (PK, auto) | ✅ | 추천 결과 식별자 |
| `userMessage` / `user_message` | `String` / `VARCHAR(500)` | ✅ | 사용자의 원문 요청 |
| `aiMessage` / `ai_message` | `String` / `TEXT` | ✅ | 사용자에게 보여 준 AI 추천 설명 |
| `placesJson` / `places_json` | `String` / `TEXT` | ✅ | `Place[]`를 JSON 문자열로 직렬화한 값, 결과가 없으면 `[]` |
| `createdAt` / `created_at` | `LocalDateTime` / `TIMESTAMP` | ✅ | 생성 시각, 저장 직전에 서버에서 설정 |

저장 정책:

- AI의 중간 검색어는 다시 조회할 필요가 없으므로 저장하지 않는다.
- API 키, 전체 프롬프트, 외부 API 원본 응답은 저장하지 않는다.
- AI·장소 검색이 모두 성공한 뒤 저장하며, 저장 실패 시 전체 요청을 실패 처리한다.

## 7. AI 연동 설계

### 7.1 프롬프트 해부학

| 파트 | 내용 |
| --- | --- |
| 역할 | “당신은 부산을 처음 방문한 자유여행객을 돕는 부산 여행 큐레이터입니다.” |
| 작업 | “사용자의 취향·지역·동행·목적을 파악해 짧은 추천 설명과 네이버 지역 검색에 사용할 구체적인 검색어를 최대 3개 만드세요. 부산과 무관하거나 위험한 요청이면 부산 여행 요청을 입력하도록 안내하세요.” |
| 출력 형식 | “`message`와 `searchQueries`를 포함하는 JSON으로 반환하세요. `message`는 한국어 두 문장 이내, `searchQueries`는 부산의 지역명과 장소 유형이 포함된 문자열 배열이며 최대 3개입니다.” |
| 사용자 입력 (동적) | `{message}` — `ChatRequest.message`의 공백을 제거한 실제 값 |

프롬프트의 동적 입력은 문자열 연결 대신 `.user(u -> u.text("{message}").param("message", message))` 형태로 전달한다.

### 7.2 구조화 출력 (평문 아님)

AI 반환 형식:

```java
public record TravelRecommendationResult(
    String message,
    List<String> searchQueries
) {}
```

호출 원칙:

```java
TravelRecommendationResult result = chatClient.prompt()
    .system(SYSTEM_PROMPT)
    .user(user -> user.text("{message}").param("message", request.message().trim()))
    .call()
    .entity(TravelRecommendationResult.class);
```

`.content()`로 평문을 받아 직접 파싱하지 않는다. `.entity(TravelRecommendationResult.class)`를 사용한다.

필드 매핑:

| AI 출력 | 사용처 |
| --- | --- |
| `TravelRecommendationResult.message` | `travel_recommendations.aiMessage`와 `ChatResponse.message` |
| `TravelRecommendationResult.searchQueries` | 네이버 지역 검색 API 입력에만 사용, DB와 프론트에는 노출하지 않음 |
| 네이버 검색 결과 | `PlaceResponse[]`로 변환해 `placesJson`과 `ChatResponse.places`에 사용 |

### 7.3 Spring AI 기능 매핑

| 우리 기능 | 쓰는 Spring AI 기능 |
| --- | --- |
| 자연어 요청을 Gemini에 전달 | 챗(`ChatClient`) |
| 추천 설명과 검색어를 안정된 Java 타입으로 변환 | 구조화 출력(`.entity(TravelRecommendationResult.class)`) |
| 날씨를 반영한 장소 추천(Should) | 툴 호출(`@Tool`)로 기상 조회 함수를 모델에 제공 |

Day 2에는 앞의 두 기능만 구현한다. 날씨 툴 호출은 핵심 호출 성공 이후에 추가한다.

### 7.4 모델·환경 설정 계약

```yaml
spring:
  ai:
    model:
      chat: google-genai
    google:
      genai:
        api-key: ${GOOGLE_API_KEY}
        chat:
          model: gemini-3.1-flash-lite
          temperature: 0.3
```

- Gradle 의존성: Spring AI BOM `2.0.0`, `org.springframework.ai:spring-ai-starter-model-google-genai`
- Gemini 키 환경변수 이름은 **`GOOGLE_API_KEY` 하나로 통일**한다.
- 네이버 서버 API 키는 `NAVER_SEARCH_CLIENT_ID`, `NAVER_SEARCH_CLIENT_SECRET`을 사용한다.
- 프론트에서 공개 가능한 지도 SDK 식별자만 `VITE_NAVER_MAP_CLIENT_ID`로 사용한다.

## 8. 예외 설계 (실패를 먼저 설계)

| 입력·실패 | 시스템 반응 |
| --- | --- |
| `message` 누락·공백 | AI를 호출하지 않고 `400` + “여행 요청을 입력해 주세요.” |
| `message` 500자 초과 | AI를 호출하지 않고 `400` + “여행 요청은 500자 이하로 입력해 주세요.” |
| AI API 호출·타임아웃·구조 변환 실패 | `500`, DB 저장 취소, “추천을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.” |
| 네이버 지역 검색 실패 | `500`, DB 저장 취소, AI 호출 실패와 동일 문구로 재시도 유도 |
| DB 저장 실패 | 트랜잭션 롤백 후 `500` + “추천 결과를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.” |
| 존재하지 않는 추천 ID | `404` + “추천 결과를 찾을 수 없습니다.” |
| API Key 누락 | 애플리케이션 시작 또는 첫 호출에서 백엔드 로그로만 확인하고 `500` 반환. 키 이름·값을 응답에 포함하지 않음 |

보안 원칙: API 키는 프론트 코드와 Git에 커밋하지 않고 백엔드 환경변수에서만 읽는다. 예외 로그에도 사용자 입력 전문, API 키, 외부 API 인증 헤더를 남기지 않는다.

## 9. 오늘의 완료 기준 (공식 Execution Checklist)

- [x] 1. 해결할 문제와 사용자가 한 문장으로 정의됨 (§1)
- [x] 2. Must 기능(3~5)과 핵심 시나리오 확정 (§2·§3)
- [x] 3. API 계약(URL·Method·JSON·오류) 문서화 (§5)
- [ ] 4. Spring AI 핵심 호출이 코드로 1회 성공 (§7 기반, 아래 `curl`로 확인)

PowerShell:

```powershell
curl.exe -X POST http://localhost:8080/api/chat `
  -H "Content-Type: application/json" `
  -d '{"message":"해운대 근처에서 혼자 가기 좋은 일식집을 알려줘"}'
```

bash:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"해운대 근처에서 혼자 가기 좋은 일식집을 알려줘"}'
```

검증 조건:

1. HTTP 상태가 `200`이다.
2. 응답에 `recommendationId`, `message`, `places`, `createdAt` 네 필드가 모두 있다.
3. `places`가 배열이고 각 원소의 필드가 `name`, `address`, `latitude`, `longitude`와 정확히 일치한다.
4. DB의 `travel_recommendations`에 같은 `recommendationId`로 한 행이 저장된다.
5. 같은 ID로 `GET /api/recommendations/{recommendationId}`를 호출했을 때 같은 응답이 반환된다.

## 10. 현재 레포 구현 정합성 체크

이 문서가 확정 계약이며, 현재 스켈레톤은 아래 항목을 구현하면서 계약에 맞춘다.

- [ ] base 패키지를 과제 기준인 `com.study`로 통일한다(현재 `com.busantrip`).
- [ ] 직접 호출용 `GeminiClient` 대신 Spring AI `ChatClient`와 Google GenAI starter를 사용한다.
- [ ] 환경변수 `GEMINI_API_KEY`를 `GOOGLE_API_KEY`로 통일한다.
- [ ] `POST /api/chat`과 `GET /api/recommendations/{recommendationId}`를 구현한다.
- [ ] 현재 `ChatResponse`에 `recommendationId`, `createdAt`을 추가하고 프론트의 `ChatResponse` 타입도 동일하게 만든다.
- [ ] JPA와 DB 드라이버를 추가하고 `travel_recommendations` Entity·Repository를 구현한다.
- [ ] Validation 오류를 포함한 모든 오류를 `{ "message": "string" }`으로 반환한다.
- [ ] 프론트 채팅 폼을 `/api/chat`에 연결하고 로딩·오류·결과·지도 마커 상태를 구현한다.

## 참고

- [Spring AI 2.0.0 ChatClient — Returning an Entity](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_returning_an_entity)
- [Spring AI Google GenAI Chat 설정](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html)

# ROAMATE

**ROAM + MATE** — 부산 여행 중 생기는 변수를 함께 정리하는 AI 여행 메이트입니다.

관광지 테마를 검색하면 현재 위치를 출발지로 삼아 방문 순서를 제안하고, 지도·경로·AI 대화로 다음 행동을 이어갈 수 있습니다. 외부 API 키가 없어도 발표와 기능 검증이 가능하도록 부산 대표 관광지, 경로, 대화의 데모 폴백을 내장했습니다.

## 핵심 사용자 흐름

1. `바다`, `야경`, `카페` 같은 테마 또는 장소명을 검색합니다.
2. 부산 관광지를 지도와 타임라인에서 확인합니다.
3. 현재 위치를 반영해 최적 방문 순서를 계산합니다.
4. AI 메이트에게 비·피로·이동수단·취향 변화에 따른 일정 조정을 요청합니다.
5. 다음 장소로 이동하며 여행 상태를 갱신합니다.

## 구현 기능

| 영역 | 기능 |
| --- | --- |
| 관광 탐색 | 관광공사 API 기반 부산 관광지 탐색, API 미설정 시 좌표가 포함된 부산 데모 카탈로그 |
| 지도 | 네이버 지도 SDK, 미설정 시에도 여행 목록과 경로 흐름을 확인할 수 있는 지도 데모 모드 |
| 경로 | 가까운 장소 우선 + 2-opt 최적화, 네이버 길찾기 API 연결 및 직선거리 폴백 |
| AI 대화 | Gemma 기반 여행 조정, 키가 없거나 호출이 실패해도 상황별 안내 답변 제공 |
| 위치 | 브라우저 GPS를 출발지로 반영, 권한이 없으면 부산시청을 기본 출발지로 사용 |

## 기술 구성

- Frontend: React 19, TypeScript, Vite, CSS
- Backend: Java 21, Spring Boot, WebFlux, Gradle
- External APIs: 한국관광공사, 네이버 지도/길찾기/지역검색, Google Generative Language(Gemma), 기상청·부산 ITS 확장 엔드포인트

현재 MVP는 별도 데이터베이스 없이 외부 데이터와 데모 카탈로그로 동작합니다. 회원·저장·즐겨찾기 기능은 다음 확장 단계의 범위입니다.

## 실행 방법

### 1. 사전 조건

- Node.js 22 이상
- Java 21 이상 JDK

### 2. 환경 변수 준비

```powershell
Copy-Item frontend/.env.example frontend/.env
Copy-Item backend/.env.example backend/.env
```

키 없이도 앱은 데모 모드로 실행됩니다. 실제 지도와 실시간 데이터를 사용하려면 각각의 API 키를 `.env`에 채우세요. `.env`는 Git에 포함되지 않습니다.

### 3. 백엔드 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

### 4. 프런트엔드 실행

```powershell
cd frontend
npm ci
npm run dev
```

브라우저에서 `http://localhost:5173`을 엽니다.

## 검증 명령

```powershell
cd frontend
npm run build
npm run lint

cd ../backend
.\gradlew.bat test
```

## 주요 API

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/system/health` | 서버 상태 확인 |
| GET | `/api/tourism/related/search` | 테마/관광지 검색 |
| POST | `/api/routes/optimize` | 장소 배열의 방문 순서와 경로 계산 |
| POST | `/api/ai/chat` | 여행 문맥 기반 대화 |
| POST | `/api/travel/search` | 자연어 여행·맛집 통합 검색 |
| GET | `/api/weather/forecast` | 기상청 예보 조회 |
| GET | `/api/traffic/avi` | 부산 ITS 교통량 조회 |

## 데모 시나리오

1. 검색창에서 `야경`을 선택합니다.
2. 추천된 장소를 확인한 뒤 **최적 경로 만들기**를 누릅니다.
3. **여행 시작하기**를 눌러 다음 장소를 이동합니다.
4. 하단의 ROAMATE AI에 `비가 오면 실내 코스로 바꿔줘`를 입력합니다.

API 키가 없는 발표 환경에서도 위 흐름 전체가 동작합니다.

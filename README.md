# 부산 AI 지도

자연어로 원하는 부산 관광지와 음식점을 검색하고 지도에 표시하는 LLM 기반 지도 서비스의 초기 프로젝트입니다. 4인 팀이 기능 개발을 바로 시작할 수 있도록 Spring Boot와 React 모노레포 구조 및 안전한 환경변수 연결 기반을 제공합니다.

## 현재 구현 범위

- 프론트엔드·백엔드 기본 구조
- 외부 API 환경변수 연결
- 백엔드 상태 및 설정 여부 확인 API
- 실제 지도·장소 검색·LLM·날씨 연동은 아직 구현하지 않음

## 필요한 API 키

| 환경변수 | 용도 | 사용 위치 |
| --- | --- | --- |
| `NAVER_MAP_CLIENT_ID` | 네이버 지도 JavaScript SDK | 프론트엔드 |
| `NAVER_SEARCH_CLIENT_ID` | 네이버 지역 검색 API 식별자 | 백엔드 |
| `NAVER_SEARCH_CLIENT_SECRET` | 네이버 지역 검색 API 인증 비밀값 | 백엔드 |
| `GEMINI_API_KEY` | Gemini LLM API 인증 | 백엔드 |
| `KMA_SERVICE_KEY` | 기상청 API 인증 | 백엔드 |

## 로컬 실행 방법

### 백엔드

필요한 환경변수를 운영체제 또는 IDE 실행 설정에 등록합니다. 값은 각자 발급받은 키를 사용하며 저장소의 파일에 작성하지 않습니다.

```text
NAVER_SEARCH_CLIENT_ID
NAVER_SEARCH_CLIENT_SECRET
GEMINI_API_KEY
KMA_SERVICE_KEY
FRONTEND_URL
```

```powershell
cd backend
.\gradlew.bat bootRun
```

macOS 또는 Linux에서는 `./gradlew bootRun`을 실행합니다. 키가 없어도 서버는 실행되며 설정 상태가 `false`로 표시됩니다.

Windows에서 한글 사용자 경로로 인해 Gradle Worker 클래스패스 오류가 발생하면, 저장소 내부의 영문 경로를 Gradle 홈으로 지정한 뒤 다시 실행합니다.

```powershell
$env:GRADLE_USER_HOME="$PWD\.gradle-user-home"
.\gradlew.bat bootRun
```

### 프론트엔드

`frontend/.env.example`을 참고해 Git에서 제외되는 `frontend/.env.local`을 직접 만들고 아래 변수만 설정합니다.

```text
VITE_NAVER_MAP_CLIENT_ID
VITE_API_BASE_URL
```

```powershell
cd frontend
npm install
npm run dev
```

지도 Client ID가 없어도 플레이스홀더 화면은 실행됩니다.

## 설정 확인 방법

```http
GET http://localhost:8080/api/system/health
GET http://localhost:8080/api/system/config-status
```

`config-status`는 키 원문·일부·길이를 반환하지 않고 설정 여부만 불리언으로 반환합니다.

## 보안 주의사항

- 실제 API 키와 `.env` 파일을 커밋하지 않습니다.
- Client Secret, Gemini 키, 기상청 키를 프론트엔드 코드나 `VITE_` 변수에 작성하지 않습니다.
- 화면 공유, 로그, 이슈, README에 실제 키를 노출하지 않습니다.

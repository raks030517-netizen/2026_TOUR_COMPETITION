# LIVE MATE 웹 프론트 적용 방법

## 1. 기존 파일 백업
현재 프로젝트의 아래 파일을 다른 폴더에 복사해두세요.

- `frontend/src/App.tsx`
- `frontend/src/styles.css`
- `frontend/src/api/systemApi.ts`
- `frontend/src/main.tsx`

## 2. 파일 복사
이 압축파일의 `src` 폴더 안 파일을 기존 프로젝트의 `frontend/src`에 덮어씁니다.

## 3. 환경변수
기존 `frontend/.env`에 아래 값을 추가하세요.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SEARCH_ENDPOINT=/api/search
VITE_NAVER_MAP_CLIENT_ID=실제_네이버지도_Client_ID
```

`VITE_SEARCH_ENDPOINT`는 실제 백엔드 컨트롤러 주소에 맞게 변경해야 합니다.

예:
- 백엔드가 `POST /api/search`라면 `/api/search`
- 백엔드가 `POST /api/chat`이라면 `/api/chat`
- 백엔드가 `POST /api/v1/travel/recommend`라면 `/api/v1/travel/recommend`

## 4. 실행
프론트엔드 터미널:

```bash
cd frontend
npm run dev
```

백엔드 터미널:

```bash
cd backend
./gradlew bootRun
```

Windows CMD/PowerShell:

```powershell
cd backend
.\gradlew.bat bootRun
```

## 5. 검색 버튼이 동작하는 이유
`App.tsx`의 `<form onSubmit={handleSearch}>`와 검색 버튼의
`type="submit"`이 연결되어 있습니다.

검색 시 `src/api/systemApi.ts`의 `searchTravelPlan()` 함수가 실행되어
백엔드로 POST 요청을 보냅니다.

## 6. 백엔드 응답 형식
현재 코드는 아래 형태를 가장 잘 처리합니다.

```json
{
  "summary": "추천 일정 설명",
  "alertTitle": "비가 35분 뒤 시작될 수 있어요",
  "alertDescription": "일정을 조정하면 더 여유롭게 즐길 수 있어요.",
  "places": [
    {
      "id": 1,
      "name": "감천문화마을",
      "description": "추천 이유",
      "arrival": "14:20 도착",
      "duration": "50분",
      "image": "https://..."
    }
  ]
}
```

백엔드 응답 키가 다르면 `src/api/systemApi.ts`의 `normalizeResult()`만 수정하면 됩니다.

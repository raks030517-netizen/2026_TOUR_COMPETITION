import { env } from '../../config/env'

export default function NaverMap() {
  return (
    <section className="map-placeholder" aria-labelledby="map-title">
      <h2 id="map-title">네이버 지도 영역</h2>
      {env.naverMapClientId ? (
        <p>NAVER_MAP_CLIENT_ID 연동 후 지도 SDK를 표시할 예정입니다.</p>
      ) : (
        <p>네이버 지도 Client ID가 설정되지 않았습니다.</p>
      )}
    </section>
  )
}


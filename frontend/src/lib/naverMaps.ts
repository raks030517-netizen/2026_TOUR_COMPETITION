declare global {
  interface Window {
    naver?: { maps: any };
    __roamateMapPromise?: Promise<void>;
  }
}

export async function loadNaverMaps(): Promise<void> {
  if (window.naver?.maps) return;
  if (window.__roamateMapPromise) return window.__roamateMapPromise;

  const clientId = import.meta.env.VITE_NAVER_MAP_CLIENT_ID;
  if (!clientId) {
    throw new Error("네이버 지도 Client ID가 설정되지 않았습니다. 데모 모드에서는 장소 목록으로 경로를 확인할 수 있습니다.");
  }

  window.__roamateMapPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("네이버 지도 SDK를 불러오지 못했습니다. Web 서비스 URL 설정을 확인해 주세요."));
    document.head.appendChild(script);
  });

  return window.__roamateMapPromise;
}

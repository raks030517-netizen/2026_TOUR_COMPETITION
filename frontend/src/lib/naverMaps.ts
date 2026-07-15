declare global { interface Window { naver?: any; __roamateMapPromise?: Promise<void>; } }
export async function loadNaverMaps(): Promise<void> {
  if (window.naver?.maps) return;
  if (window.__roamateMapPromise) return window.__roamateMapPromise;
  const key = import.meta.env.VITE_NAVER_MAP_CLIENT_ID;
  if (!key) throw new Error("VITE_NAVER_MAP_CLIENT_ID가 없습니다.");
  window.__roamateMapPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(key)}`;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("네이버 지도 로딩 실패"));
    document.head.appendChild(script);
  });
  return window.__roamateMapPromise;
}

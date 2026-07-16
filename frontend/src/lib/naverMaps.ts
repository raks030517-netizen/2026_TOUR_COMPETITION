const NAVER_MAP_SCRIPT_ID = "naver-dynamic-map-sdk";

declare global { interface Window { naver?: any; __roamateMapPromise?: Promise<void>; } }
export async function loadNaverMaps(): Promise<void> {
  if (window.naver?.maps) return;
  if (window.__roamateMapPromise) return window.__roamateMapPromise;
  const key = import.meta.env.VITE_NAVER_MAP_CLIENT_ID;
  if (!key) throw new Error("VITE_NAVER_MAP_CLIENT_ID가 없습니다.");
  window.__roamateMapPromise = new Promise<void>((resolve, reject) => {
    const existingScript = document.getElementById(
      NAVER_MAP_SCRIPT_ID,
    ) as HTMLScriptElement | null;
    const script = existingScript ?? document.createElement("script");
    script.id = NAVER_MAP_SCRIPT_ID;
    script.async = true;
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(key)}&submodules=geocoder`;
    script.onload = () => {
      if (window.naver?.maps) {
        resolve();
      } else {
        window.__roamateMapPromise = undefined;
        reject(new Error("네이버 지도 SDK를 초기화하지 못했습니다."));
      }
    };
    script.onerror = () => {
      window.__roamateMapPromise = undefined;
      script.remove();
      reject(new Error(
        "네이버 동적 지도를 불러오지 못했습니다. Client ID와 Web 서비스 URL을 확인해 주세요.",
      ));
    };
    if (!existingScript) document.head.appendChild(script);
  });
  return window.__roamateMapPromise;
}

export async function geocodeAddress(
  query: string,
): Promise<{ latitude: number; longitude: number } | null> {
  await loadNaverMaps();

  return new Promise((resolve, reject) => {
    const service = window.naver?.maps?.Service;
    if (!service?.geocode) {
      reject(new Error("네이버 지도 주소 검색 기능을 사용할 수 없습니다."));
      return;
    }

    service.geocode({ query }, (status: string, response: any) => {
      if (status !== service.Status.OK) {
        resolve(null);
        return;
      }

      const address = response?.v2?.addresses?.[0];
      const latitude = Number(address?.y);
      const longitude = Number(address?.x);
      resolve(
        Number.isFinite(latitude) && Number.isFinite(longitude)
          ? { latitude, longitude }
          : null,
      );
    });
  });
}

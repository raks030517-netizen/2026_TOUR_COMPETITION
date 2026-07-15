/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_NAVER_MAP_CLIENT_ID?: string;
  readonly VITE_DEFAULT_BASE_YM?: string;
  readonly VITE_DEFAULT_SIGNGU_CD?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

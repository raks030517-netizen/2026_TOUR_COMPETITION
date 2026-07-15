import type { TourismSearchParams } from "../types/tourism";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") ??
  "http://localhost:8080";

export async function searchTourism({
  baseYm,
  signguCd,
  keyword,
  pageNo = 1,
  numOfRows = 10,
}: TourismSearchParams): Promise<unknown> {
  const params = new URLSearchParams({
    baseYm,
    signguCd,
    keyword,
    pageNo: String(pageNo),
    numOfRows: String(numOfRows),
  });

  const response = await fetch(
    `${API_BASE_URL}/api/tourism/related/search?${params.toString()}`,
    {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    },
  );

  const text = await response.text();

  if (!response.ok) {
    throw new Error(
      `관광지 검색 실패 (${response.status}): ${text || "응답 없음"}`,
    );
  }

  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

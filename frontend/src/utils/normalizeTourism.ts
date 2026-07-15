import type { TourismPlace } from "../types/tourism";

type RawObject = Record<string, unknown>;

const FALLBACK_IMAGES = [
  "https://images.unsplash.com/photo-1517154421773-0529f29ea451?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1588416936097-41850ab3d86d?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=1200&q=80",
];

export function normalizeTourismResponse(response: unknown): TourismPlace[] {
  const items = findItems(response);

  return items.map((item, index) => ({
    id:
      readString(item, [
        "id",
        "contentid",
        "contentId",
        "tourId",
        "rlteTatsCd",
      ]) || String(index + 1),

    name:
      readString(item, [
        "title",
        "name",
        "tourNm",
        "tourismName",
        "relateTourNm",
        "rlteTatsNm",
        "contenttitle",
      ]) || `추천 관광지 ${index + 1}`,

    description:
      readString(item, [
        "description",
        "overview",
        "summary",
        "reason",
        "rlteTatsIntrcn",
        "tourDescription",
      ]) || "함께 둘러보기 좋은 연관 관광지입니다.",

    address:
      readString(item, [
        "address",
        "addr1",
        "addr",
        "roadAddress",
        "location",
        "signguNm",
        "rlteBsicAdres",
      ]) || "부산광역시",

    image:
      readString(item, [
        "image",
        "imageUrl",
        "firstimage",
        "firstImage",
        "firstimage2",
        "thumbnail",
        "imgUrl",
        "rlteTatsImg",
      ]) || FALLBACK_IMAGES[index % FALLBACK_IMAGES.length],

    category:
      readString(item, [
        "category",
        "cat1",
        "cat2",
        "cat3",
        "contentTypeName",
        "rlteCtgryLclsNm",
      ]) || "연관 관광지",

    distance:
      readString(item, [
        "distance",
        "dist",
        "rlteTatsDstnc",
      ]) || "거리 정보 확인 중",

    latitude: readNumber(item, [
      "latitude",
      "lat",
      "mapy",
      "y",
      "rlteTatsYcrd",
    ]),

    longitude: readNumber(item, [
      "longitude",
      "lng",
      "lon",
      "mapx",
      "x",
      "rlteTatsXcrd",
    ]),
  }));
}

function findItems(value: unknown): RawObject[] {
  if (Array.isArray(value)) {
    return value.filter(isObject);
  }

  if (!isObject(value)) return [];

  const priorityKeys = [
    "item",
    "items",
    "results",
    "result",
    "data",
    "list",
    "tourism",
    "places",
    "body",
    "response",
  ];

  for (const key of priorityKeys) {
    const result = findItems(value[key]);
    if (result.length > 0) return result;
  }

  for (const child of Object.values(value)) {
    const result = findItems(child);
    if (result.length > 0) return result;
  }

  return [];
}

function isObject(value: unknown): value is RawObject {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readString(object: RawObject, keys: string[]): string {
  for (const key of keys) {
    const value = object[key];

    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }

    if (typeof value === "number") {
      return String(value);
    }
  }

  return "";
}

function readNumber(
  object: RawObject,
  keys: string[],
): number | undefined {
  for (const key of keys) {
    const value = object[key];
    const number =
      typeof value === "number"
        ? value
        : typeof value === "string"
          ? Number(value)
          : Number.NaN;

    if (Number.isFinite(number)) return number;
  }

  return undefined;
}

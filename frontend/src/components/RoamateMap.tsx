import { useEffect, useMemo, useRef, useState } from "react";
import { loadNaverMaps } from "../lib/naverMaps";
import type { Coordinate, RouteResponse, TourismPlace } from "../types";
import "./RoamateMap.css";

interface Props {
  currentLocation: Coordinate;
  accuracy?: number;
  places: TourismPlace[];
  selectedId?: string;
  route?: RouteResponse;
  onSelect: (place: TourismPlace) => void;
}

type MapStatus = "loading" | "ready" | "error";

function hasCoordinate(coordinate: Coordinate): boolean {
  return Number.isFinite(coordinate.latitude)
    && Number.isFinite(coordinate.longitude);
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function infoWindowContent(place: TourismPlace): string {
  return `
    <article class="map-info-window">
      <strong>${escapeHtml(place.name)}</strong>
      <span>${escapeHtml(place.category || "관광지")}</span>
      <p>${escapeHtml(place.address || "주소 정보 없음")}</p>
    </article>
  `;
}

function detachOverlays(overlays: any[]): void {
  overlays.forEach((overlay) => overlay?.setMap?.(null));
  overlays.length = 0;
}

function clearListeners(listeners: any[]): void {
  const event = window.naver?.maps?.Event;
  listeners.forEach((listener) => event?.removeListener?.(listener));
  listeners.length = 0;
}

export default function RoamateMap({
  currentLocation,
  accuracy,
  places,
  selectedId,
  route,
  onSelect,
}: Props) {
  const elementRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const initialCenterRef = useRef(currentLocation);
  const infoWindowRef = useRef<any>(null);
  const startOverlaysRef = useRef<any[]>([]);
  const placeMarkersRef = useRef<any[]>([]);
  const destinationMarkerRef = useRef<any>(null);
  const routePolylinesRef = useRef<any[]>([]);
  const placeListenersRef = useRef<any[]>([]);
  const destinationListenerRef = useRef<any>(null);
  const [status, setStatus] = useState<MapStatus>("loading");
  const [error, setError] = useState("");

  const displayedPlaces = places;
  const routePaths = useMemo(
    () => route?.path ?? [],
    [route?.path],
  );
  const routeCoordinates = useMemo(
    () => routePaths.flatMap((segment) => segment.coordinates),
    [routePaths],
  );
  const selectedPlace = useMemo(
    () => displayedPlaces.find((place) => place.id === selectedId),
    [displayedPlaces, selectedId],
  );

  // Dynamic Map SDK를 로드하고 지도 인스턴스를 한 번만 생성한다.
  useEffect(() => {
    let active = true;
    const placeListeners = placeListenersRef.current;
    const startOverlays = startOverlaysRef.current;
    const placeMarkers = placeMarkersRef.current;
    const routePolylines = routePolylinesRef.current;

    loadNaverMaps()
      .then(() => {
        if (!active || !elementRef.current || mapRef.current) return;

        const { maps } = window.naver;
        const center = initialCenterRef.current;
        mapRef.current = new maps.Map(elementRef.current, {
          center: new maps.LatLng(center.latitude, center.longitude),
          zoom: 13,
          minZoom: 8,
          draggable: true,
          scrollWheel: true,
          pinchZoom: true,
          keyboardShortcuts: true,
          mapDataControl: false,
          scaleControl: true,
          zoomControl: false,
        });
        infoWindowRef.current = new maps.InfoWindow({
          borderWidth: 0,
          backgroundColor: "transparent",
          disableAnchor: true,
          pixelOffset: new maps.Point(0, -12),
        });
        setStatus("ready");
      })
      .catch((caughtError: unknown) => {
        if (!active) return;
        setError(
          caughtError instanceof Error
            ? caughtError.message
            : "네이버 동적 지도를 불러오지 못했습니다.",
        );
        setStatus("error");
      });

    return () => {
      active = false;
      infoWindowRef.current?.close?.();
      clearListeners(placeListeners);
      if (destinationListenerRef.current) {
        window.naver?.maps?.Event?.removeListener?.(
          destinationListenerRef.current,
        );
        destinationListenerRef.current = null;
      }
      detachOverlays(startOverlays);
      detachOverlays(placeMarkers);
      destinationMarkerRef.current?.setMap?.(null);
      detachOverlays(routePolylines);
      if (mapRef.current && window.naver?.maps?.Event) {
        window.naver.maps.Event.clearInstanceListeners(mapRef.current);
      }
      mapRef.current = null;
    };
  }, []);

  // 현재 위치를 출발지로 표시한다.
  useEffect(() => {
    if (status !== "ready" || !mapRef.current || !hasCoordinate(currentLocation)) {
      return;
    }

    detachOverlays(startOverlaysRef.current);
    const { maps } = window.naver;
    const position = new maps.LatLng(
      currentLocation.latitude,
      currentLocation.longitude,
    );
    const circle = new maps.Circle({
      map: mapRef.current,
      center: position,
      radius: Math.min(Math.max(accuracy ?? 80, 35), 500),
      fillColor: "#2d84ff",
      fillOpacity: 0.12,
      strokeColor: "#2d84ff",
      strokeOpacity: 0.35,
      strokeWeight: 1,
    });
    const marker = new maps.Marker({
      map: mapRef.current,
      position,
      title: "출발지",
      icon: {
        content: '<div class="route-endpoint-marker start"><span></span><b>출발</b></div>',
        anchor: new maps.Point(24, 24),
      },
      zIndex: 500,
    });
    startOverlaysRef.current.push(circle, marker);
  }, [accuracy, currentLocation, status]);

  // 일반 장소 마커와 클릭 정보창을 관리한다.
  useEffect(() => {
    if (status !== "ready" || !mapRef.current) return;

    const placeListeners = placeListenersRef.current;
    const placeMarkers = placeMarkersRef.current;
    infoWindowRef.current?.close?.();
    clearListeners(placeListeners);
    detachOverlays(placeMarkers);
    const { maps } = window.naver;

    displayedPlaces
      .filter((place) => place.id !== selectedId && hasCoordinate(place))
      .forEach((place, index) => {
        const marker = new maps.Marker({
          map: mapRef.current,
          position: new maps.LatLng(place.latitude, place.longitude),
          title: place.name,
          icon: {
            content: `<button class="place-marker" type="button" aria-label="${escapeHtml(place.name)}"><span>${index + 1}</span></button>`,
            anchor: new maps.Point(24, 48),
          },
          zIndex: 300 - index,
        });
        const listener = maps.Event.addListener(marker, "click", () => {
          onSelect(place);
          infoWindowRef.current?.setContent?.(infoWindowContent(place));
          infoWindowRef.current?.open?.(mapRef.current, marker);
        });
        placeMarkers.push(marker);
        placeListeners.push(listener);
      });

    return () => {
      clearListeners(placeListeners);
      detachOverlays(placeMarkers);
    };
  }, [displayedPlaces, onSelect, selectedId, status]);

  // 선택 장소를 도착지 마커로 분리해 강조한다.
  useEffect(() => {
    if (status !== "ready" || !mapRef.current) return;

    destinationMarkerRef.current?.setMap?.(null);
    destinationMarkerRef.current = null;
    if (!selectedPlace || !hasCoordinate(selectedPlace)) return;

    const { maps } = window.naver;
    const marker = new maps.Marker({
      map: mapRef.current,
      position: new maps.LatLng(
        selectedPlace.latitude,
        selectedPlace.longitude,
      ),
      title: `도착지: ${selectedPlace.name}`,
      icon: {
        content: '<div class="route-endpoint-marker destination"><span></span><b>도착</b></div>',
        anchor: new maps.Point(24, 48),
      },
      zIndex: 600,
    });
    const listener = maps.Event.addListener(marker, "click", () => {
      onSelect(selectedPlace);
      infoWindowRef.current?.setContent?.(infoWindowContent(selectedPlace));
      infoWindowRef.current?.open?.(mapRef.current, marker);
    });
    destinationMarkerRef.current = marker;
    destinationListenerRef.current = listener;

    return () => {
      maps.Event.removeListener(listener);
      if (destinationListenerRef.current === listener) {
        destinationListenerRef.current = null;
      }
      marker.setMap(null);
      if (destinationMarkerRef.current === marker) {
        destinationMarkerRef.current = null;
      }
    };
  }, [onSelect, selectedPlace, status]);

  // 실제 좌표가 제공된 구간별로 Polyline을 분리해, 좌표가 없는 구간을 직선으로 잇지 않는다.
  useEffect(() => {
    if (status !== "ready" || !mapRef.current) return;

    detachOverlays(routePolylinesRef.current);
    const { maps } = window.naver;
    const colors: Record<string, string> = {
      DRIVE: "#6f4cff",
      BUS: "#38a7ff",
      SUBWAY: "#ff617c",
      WALK: "#8590a8",
    };
    routePaths.forEach((segment) => {
      const coordinates = segment.coordinates.filter(hasCoordinate);
      if (coordinates.length < 2) return;
      const path = coordinates.map(
        (coordinate) => new maps.LatLng(
          coordinate.latitude,
          coordinate.longitude,
        ),
      );
      const glow = new maps.Polyline({
        map: mapRef.current,
        path,
        strokeColor: "#ffffff",
        strokeWeight: 11,
        strokeOpacity: 0.75,
        strokeLineCap: "round",
        strokeLineJoin: "round",
      });
      const line = new maps.Polyline({
        map: mapRef.current,
        path,
        strokeColor: colors[segment.type] ?? "#6f4cff",
        strokeWeight: 6,
        strokeOpacity: 0.96,
        strokeLineCap: "round",
        strokeLineJoin: "round",
      });
      routePolylinesRef.current.push(glow, line);
    });
  }, [routePaths, status]);

  // 장소 또는 경로가 바뀌면 관련 좌표가 모두 보이도록 범위를 조정한다.
  useEffect(() => {
    if (status !== "ready" || !mapRef.current) return;

    const { maps } = window.naver;
    const coordinates = [
      currentLocation,
      ...displayedPlaces,
      ...routeCoordinates,
    ].filter(hasCoordinate);
    if (coordinates.length === 0) return;
    if (coordinates.length === 1) {
      mapRef.current.setCenter(
        new maps.LatLng(coordinates[0].latitude, coordinates[0].longitude),
      );
      mapRef.current.setZoom(13);
      return;
    }

    const bounds = new maps.LatLngBounds();
    coordinates.forEach((coordinate) => bounds.extend(
      new maps.LatLng(coordinate.latitude, coordinate.longitude),
    ));
    mapRef.current.fitBounds(bounds, {
      top: 110,
      right: 100,
      bottom: 220,
      left: 90,
    });
  }, [currentLocation, displayedPlaces, routeCoordinates, status]);

  return (
    <div className="map-wrap">
      <div ref={elementRef} className="naver-map" aria-label="부산 관광 동적 지도" />

      {status === "loading" && (
        <div className="map-loading" role="status">
          네이버 동적 지도를 불러오는 중입니다.
        </div>
      )}
      {status === "error" && (
        <div className="map-error" role="alert">
          <strong>네이버 지도 오류</strong>
          <span>{error}</span>
        </div>
      )}

      <div className="map-controls" aria-label="지도 확대 및 축소">
        <button
          type="button"
          aria-label="현재 위치로 이동"
          onClick={() => {
            if (!mapRef.current || !window.naver?.maps) return;
            mapRef.current.panTo(new window.naver.maps.LatLng(
              currentLocation.latitude,
              currentLocation.longitude,
            ));
            mapRef.current.setZoom(14);
          }}
        >
          ◎
        </button>
        <button
          type="button"
          aria-label="지도 확대"
          onClick={() => mapRef.current?.setZoom(mapRef.current.getZoom() + 1)}
        >
          ＋
        </button>
        <button
          type="button"
          aria-label="지도 축소"
          onClick={() => mapRef.current?.setZoom(mapRef.current.getZoom() - 1)}
        >
          －
        </button>
      </div>
    </div>
  );
}

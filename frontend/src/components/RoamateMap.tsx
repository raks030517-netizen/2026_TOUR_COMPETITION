import { useEffect, useRef, useState } from "react";
import { loadNaverMaps } from "../lib/naverMaps";
import type { Coordinate, OptimizedRoute, TourismPlace } from "../types";

interface Props {
  currentLocation: Coordinate;
  accuracy?: number;
  places: TourismPlace[];
  selectedId?: string;
  route?: OptimizedRoute;
  onSelect: (place: TourismPlace) => void;
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
  const overlaysRef = useRef<any[]>([]);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    void loadNaverMaps()
      .then(() => {
        if (!active || !elementRef.current || !window.naver?.maps) return;
        mapRef.current = new window.naver.maps.Map(elementRef.current, {
          center: new window.naver.maps.LatLng(currentLocation.latitude, currentLocation.longitude),
          zoom: 13,
          minZoom: 8,
          mapDataControl: false,
          zoomControl: false,
          scaleControl: false,
        });
        setReady(true);
      })
      .catch((caughtError: unknown) => {
        if (active) setError(caughtError instanceof Error ? caughtError.message : "지도를 준비하지 못했습니다.");
      });

    return () => {
      active = false;
      overlaysRef.current.forEach((overlay) => overlay.setMap?.(null));
    };
  }, [currentLocation.latitude, currentLocation.longitude]);

  useEffect(() => {
    if (!ready || !mapRef.current || !window.naver?.maps) return;

    overlaysRef.current.forEach((overlay) => overlay.setMap?.(null));
    overlaysRef.current = [];

    const maps = window.naver.maps;
    const bounds = new maps.LatLngBounds();
    const current = new maps.LatLng(currentLocation.latitude, currentLocation.longitude);
    bounds.extend(current);

    const circle = new maps.Circle({
      map: mapRef.current,
      center: current,
      radius: Math.min(Math.max(accuracy ?? 80, 35), 500),
      fillColor: "#2d84ff",
      fillOpacity: 0.12,
      strokeColor: "#2d84ff",
      strokeOpacity: 0.35,
      strokeWeight: 1,
    });
    const currentMarker = new maps.Marker({
      map: mapRef.current,
      position: current,
      icon: {
        content: '<div class="current-location-marker"><span></span><b>현재 위치</b></div>',
        anchor: new maps.Point(24, 24),
      },
      zIndex: 400,
    });
    overlaysRef.current.push(circle, currentMarker);

    const displayPlaces = route?.orderedPlaces.length ? route.orderedPlaces : places;
    displayPlaces.forEach((place, index) => {
      const position = new maps.LatLng(place.latitude, place.longitude);
      bounds.extend(position);
      const marker = new maps.Marker({
        map: mapRef.current,
        position,
        title: place.name,
        icon: {
          content: `<button class="place-marker ${selectedId === place.id ? "selected" : ""}" type="button"><span>${index + 1}</span></button>`,
          anchor: new maps.Point(24, 48),
        },
        zIndex: 300 - index,
      });
      maps.Event.addListener(marker, "click", () => onSelect(place));
      overlaysRef.current.push(marker);
    });

    if (route?.path.length) {
      const path = route.path.map((point) => new maps.LatLng(point.latitude, point.longitude));
      path.forEach((point) => bounds.extend(point));
      overlaysRef.current.push(
        new maps.Polyline({
          map: mapRef.current,
          path,
          strokeColor: "#ffffff",
          strokeWeight: 11,
          strokeOpacity: 0.72,
          strokeLineCap: "round",
          strokeLineJoin: "round",
        }),
        new maps.Polyline({
          map: mapRef.current,
          path,
          strokeColor: "#6f4cff",
          strokeWeight: 6,
          strokeOpacity: 0.96,
          strokeLineCap: "round",
          strokeLineJoin: "round",
        }),
      );
    }

    mapRef.current.fitBounds(bounds, { top: 92, right: 72, bottom: 220, left: 72 });
  }, [accuracy, currentLocation, onSelect, places, ready, route, selectedId]);

  const recenter = () => {
    if (!mapRef.current || !window.naver?.maps) return;
    mapRef.current.panTo(new window.naver.maps.LatLng(currentLocation.latitude, currentLocation.longitude));
    mapRef.current.setZoom(14);
  };

  return (
    <div className="map-wrap">
      <div ref={elementRef} className="naver-map" />
      {error && (
        <div className="map-error">
          <strong>지도 데모 모드</strong>
          <span>{error}</span>
        </div>
      )}
      <div className="map-controls" aria-label="지도 제어">
        <button type="button" onClick={recenter} aria-label="현재 위치로 이동">◎</button>
        <button type="button" onClick={() => mapRef.current?.setZoom(mapRef.current.getZoom() + 1)} aria-label="확대">+</button>
        <button type="button" onClick={() => mapRef.current?.setZoom(mapRef.current.getZoom() - 1)} aria-label="축소">−</button>
      </div>
    </div>
  );
}

import { useEffect, useRef, useState } from "react";
import { geocodeAddress, loadNaverMaps } from "../lib/naverMaps";
import type { TourismPlace } from "../types/tourism";

interface Props {
  places: TourismPlace[];
  selectedId?: string;
  onSelect: (place: TourismPlace) => void;
}

const BUSAN = {
  latitude: 35.1795543,
  longitude: 129.0756416,
};

export default function NaverMap({
  places,
  selectedId,
  onSelect,
}: Props) {
  const mapElementRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const markerRefs = useRef<any[]>([]);
  const polylineRef = useRef<any>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function init() {
      try {
        await loadNaverMaps();

        if (
          cancelled ||
          !mapElementRef.current ||
          mapRef.current
        ) {
          return;
        }

        mapRef.current = new window.naver.maps.Map(
          mapElementRef.current,
          {
            center: new window.naver.maps.LatLng(
              BUSAN.latitude,
              BUSAN.longitude,
            ),
            zoom: 12,
            minZoom: 8,
            zoomControl: false,
            mapDataControl: false,
            scaleControl: false,
          },
        );
      } catch (e) {
        setError(
          e instanceof Error
            ? e.message
            : "지도를 불러오지 못했습니다.",
        );
      }
    }

    void init();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function renderMarkers() {
      if (!mapRef.current || !window.naver?.maps) return;

      markerRefs.current.forEach((marker) => marker.setMap(null));
      markerRefs.current = [];

      if (polylineRef.current) {
        polylineRef.current.setMap(null);
        polylineRef.current = null;
      }

      const resolvedPlaces = await Promise.all(
        places.slice(0, 6).map(async (place) => {
          if (
            Number.isFinite(place.latitude) &&
            Number.isFinite(place.longitude)
          ) {
            return place;
          }

          const geocoded = await geocodeAddress(
            `${place.address} ${place.name}`,
          );

          return geocoded
            ? { ...place, ...geocoded }
            : place;
        }),
      );

      if (cancelled) return;

      const drawable = resolvedPlaces.filter(
        (place) =>
          Number.isFinite(place.latitude) &&
          Number.isFinite(place.longitude),
      );

      if (drawable.length === 0) return;

      const bounds = new window.naver.maps.LatLngBounds();
      const path: any[] = [];

      drawable.forEach((place, index) => {
        const position = new window.naver.maps.LatLng(
          place.latitude,
          place.longitude,
        );

        bounds.extend(position);
        path.push(position);

        const marker = new window.naver.maps.Marker({
          map: mapRef.current,
          position,
          title: place.name,
          icon: {
            content: `
              <button class="roamate-marker ${
                selectedId === place.id ? "selected" : ""
              }" type="button">${index + 1}</button>
            `,
            anchor: new window.naver.maps.Point(23, 46),
          },
        });

        window.naver.maps.Event.addListener(
          marker,
          "click",
          () => onSelect(place),
        );

        markerRefs.current.push(marker);
      });

      if (path.length >= 2) {
        polylineRef.current = new window.naver.maps.Polyline({
          map: mapRef.current,
          path,
          strokeColor: "#6f4cff",
          strokeWeight: 5,
          strokeOpacity: 0.9,
          strokeStyle: "shortdash",
        });
      }

      if (drawable.length === 1) {
        mapRef.current.setCenter(path[0]);
        mapRef.current.setZoom(14);
      } else {
        mapRef.current.fitBounds(bounds, {
          top: 90,
          right: 90,
          bottom: 120,
          left: 90,
        });
      }
    }

    void renderMarkers();

    return () => {
      cancelled = true;
    };
  }, [places, selectedId, onSelect]);

  return (
    <div className="map-wrap">
      <div ref={mapElementRef} className="naver-map" />

      {error && (
        <div className="map-error">
          <strong>네이버 지도 오류</strong>
          <span>{error}</span>
        </div>
      )}

      <div className="map-controls">
        <button
          type="button"
          onClick={() => {
            if (!mapRef.current || !window.naver?.maps) return;

            mapRef.current.setCenter(
              new window.naver.maps.LatLng(
                BUSAN.latitude,
                BUSAN.longitude,
              ),
            );
            mapRef.current.setZoom(12);
          }}
        >
          ◎
        </button>

        <button
          type="button"
          onClick={() =>
            mapRef.current?.setZoom(
              mapRef.current.getZoom() + 1,
            )
          }
        >
          ＋
        </button>

        <button
          type="button"
          onClick={() =>
            mapRef.current?.setZoom(
              mapRef.current.getZoom() - 1,
            )
          }
        >
          －
        </button>
      </div>
    </div>
  );
}

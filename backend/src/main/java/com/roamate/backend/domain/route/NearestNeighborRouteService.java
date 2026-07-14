package com.roamate.backend.domain.route;

import com.roamate.backend.domain.place.Place;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NearestNeighborRouteService implements SmartRouteService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public List<Place> optimizeOrder(List<Place> places, Double startLatitude, Double startLongitude) {
        List<Place> remaining = new ArrayList<>(places);
        List<Place> ordered = new ArrayList<>();

        double currentLat = startLatitude != null ? startLatitude : remaining.get(0).getLatitude();
        double currentLon = startLongitude != null ? startLongitude : remaining.get(0).getLongitude();

        while (!remaining.isEmpty()) {
            double finalLat = currentLat;
            double finalLon = currentLon;
            Place nearest = remaining.stream()
                    .min((a, b) -> Double.compare(
                            distanceKm(finalLat, finalLon, a.getLatitude(), a.getLongitude()),
                            distanceKm(finalLat, finalLon, b.getLatitude(), b.getLongitude())))
                    .orElseThrow();

            ordered.add(nearest);
            remaining.remove(nearest);
            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
        }

        return ordered;
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

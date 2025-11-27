package org.example.localy.util;

public class DistanceCalculator {

    private static final double EARTH_RADIUS = 6371.0; // 지구 반지름 (km)

    // 두 좌표 간 거리 계산 (km)
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // 거리를 반올림하여 소수점 첫째자리까지 반환
    public static double roundDistance(double distance) {
        return Math.round(distance * 10.0) / 10.0;
    }
}
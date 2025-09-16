package runrush.be.common.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class GeoUtils {
    private static final int EARTH_RADIUS_KM = 6371; // 지구 반지름 (km)
    private static final double SIGNIFICANT_MOVEMENT_THRESHOLD_M = 50.0; // (m)

    /**
     * 두 지점 간의 거리를 계산 (Haversine 공식)
     *
     * @param lat1 시작점 위도
     * @param lng1 시작점 경도
     * @param lat2 종료점 위도
     * @param lng2 종료점 경도
     * @return 거리 (km)
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 두 지점 간의 거리를 계산 (미터 단위)
     *
     * @param lat1 시작점 위도
     * @param lng1 시작점 경도
     * @param lat2 종료점 위도
     * @param lng2 종료점 경도
     * @return 거리 (m)
     */
    public static double calculateDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        return calculateDistance(lat1, lng1, lat2, lng2) * 1000;
    }

    /**
     * 의미있는 움직임인지 판단 (50미터 이상)
     *
     * @param lat1 이전 위도
     * @param lng1 이전 경도
     * @param lat2 현재 위도
     * @param lng2 현재 경도
     * @return 50미터 이상 움직였으면 true
     */
    public static boolean hasSignificantMovement(double lat1, double lng1, double lat2, double lng2) {
        double distanceInMeters = calculateDistanceInMeters(lat1, lng1, lat2, lng2);
        return distanceInMeters > SIGNIFICANT_MOVEMENT_THRESHOLD_M;
    }

    /**
     * 좌표 유효성 검증
     *
     * @param latitude  위도 (-90 ~ 90)
     * @param longitude 경도 (-180 ~ 180)
     * @return 유효하면 true
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }
}
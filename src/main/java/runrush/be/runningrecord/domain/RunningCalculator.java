package runrush.be.runningrecord.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.common.util.GeoUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RunningCalculator {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int EXPERIENCE_PER_10_METERS = 1;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int METERS_PER_KILOMETER = 1000;

    /**
     * GeoJSON 경로 데이터로부터 총 거리를 계산합니다.
     */
    public double calculateTotalDistance(String pathGeoJson) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(pathGeoJson);
            
            validateGeoJsonFormat(jsonNode);
            
            JsonNode coordinates = jsonNode.get("coordinates");
            double totalDistance = 0.0;
            
            for (int i = 1; i < coordinates.size(); i++) {
                JsonNode prev = coordinates.get(i - 1);
                JsonNode curr = coordinates.get(i);
                
                double lon1 = prev.get(0).asDouble();
                double lat1 = prev.get(1).asDouble();
                double lon2 = curr.get(0).asDouble();
                double lat2 = curr.get(1).asDouble();
                
                totalDistance += GeoUtils.calculateDistanceInMeters(lat1, lon1, lat2, lon2);
            }
            
            return totalDistance;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
        }
    }

    /**
     * 총 거리로부터 경험치를 계산합니다.
     */
    public int calculateExperiencePoints(double totalDistanceInMeters) {
        return (int) (totalDistanceInMeters / 10) * EXPERIENCE_PER_10_METERS;
    }

    /**
     * 거리와 시간으로부터 페이스를 계산합니다. (분/km)
     */
    public double calculatePace(double totalDistanceInMeters, long totalTimeInSeconds) {
        if (totalDistanceInMeters <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
        }
        
        BigDecimal minutes = BigDecimal.valueOf(totalTimeInSeconds)
                .divide(BigDecimal.valueOf(SECONDS_PER_MINUTE), 10, RoundingMode.HALF_UP);
        BigDecimal kilometers = BigDecimal.valueOf(totalDistanceInMeters)
                .divide(BigDecimal.valueOf(METERS_PER_KILOMETER), 10, RoundingMode.HALF_UP);
        
        return minutes.divide(kilometers, 2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 러닝 시간이 유효한지 검증합니다.
     */
    public void validateRunningTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_RUNNING_TIME);
        }
        
        long duration = Duration.between(startTime, endTime).getSeconds();
        if (duration <= 0) {
            throw new BusinessException(ErrorCode.INVALID_RUNNING_TIME);
        }
    }

    /**
     * 총 러닝 시간을 초 단위로 계산합니다.
     */
    public long calculateTotalTimeInSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        validateRunningTime(startTime, endTime);
        return Duration.between(startTime, endTime).getSeconds();
    }

    private void validateGeoJsonFormat(JsonNode jsonNode) {
        String type = jsonNode.get("type").asText();
        if (!"LineString".equals(type)) {
            throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
        }
        
        JsonNode coordinates = jsonNode.get("coordinates");
        if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
        }
    }
}
package runrush.be.runningrecord.dto;

import java.time.LocalDateTime;

public record RunningRecordRequest(
        String email,
        String pathGeoJson,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        LocalDateTime startedTime,
        LocalDateTime endedTime
) {
}
package runrush.be.runningrecord.dto;

import com.fasterxml.jackson.databind.JsonNode;
import runrush.be.common.util.GeoJsonUtil;
import runrush.be.runningrecord.domain.RunningRecord;

import java.time.LocalDateTime;

public record RunningRecordResponse(
        Long id,
        double totalDistance,
        long totalTime,
        double pace,
        LocalDateTime startedTime,
        LocalDateTime endedTime,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        String startLocationName,
        String endLocationName,
        JsonNode pathGeoJson
) {
    public static RunningRecordResponse toRecordResponse(RunningRecord record) {
        return new RunningRecordResponse(
                record.getId(),
                record.getTotalDistance(),
                record.getTotalTime(),
                record.getPace(),
                record.getStartedTime(),
                record.getEndedTime(),
                record.getStartLatitude(),
                record.getStartLongitude(),
                record.getEndLatitude(),
                record.getEndLongitude(),
                record.getStartLocationName(),
                record.getEndLocationName(),
                GeoJsonUtil.parseGeoJson(record.getPathGeoJson())
        );
    }
}
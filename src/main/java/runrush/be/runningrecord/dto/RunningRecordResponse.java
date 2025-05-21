package runrush.be.runningrecord.dto;

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
        String pathGeoJson
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
                record.getPathGeoJson()
        );
    }
}
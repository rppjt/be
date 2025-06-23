package runrush.be.runningrecord.dto;

import runrush.be.runningrecord.domain.RunningRecord;

import java.time.LocalDateTime;

public record RunningRecordListResponse(
        Long id,
        double totalDistance,
        long totalTime,
        double pace,
        String endLocationName,
        LocalDateTime createAt,
        RecommendedCourseInfo recommendedCourse
) {
    public static RunningRecordListResponse toRecordListResponse(RunningRecord record) {
        return new RunningRecordListResponse(
                record.getId(),
                Math.round(record.getTotalDistance() * 100.0) / 100.0,
                record.getTotalTime(),
                Math.round(record.getPace() * 100.0) / 100.0,
                record.getEndLocationName(),
                record.getCreatedAt(),
                RecommendedCourseInfo.from(record.getRecommendedCourse())
        );
    }
}
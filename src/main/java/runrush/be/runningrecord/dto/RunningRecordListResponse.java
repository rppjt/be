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
                record.getTotalDistance(),
                record.getTotalTime(),
                record.getPace(),
                record.getEndLocationName(),
                record.getCreatedAt(),
                RecommendedCourseInfo.from(record.getRecommendedCourse())
        );
    }
}
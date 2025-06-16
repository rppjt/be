package runrush.be.stats.dto;

import java.util.List;

public record RecommendedCourseDetailStatsResponse(
        Long courseId,
        String courseTitle,
        String creatorName,
        double courseDistanceKm,

        int totalCompletionCount,
        int uniqueRunnerCount,
        double averageCompletionTimeSeconds,
        double averagePace,

        int myCompletionCount,
        Double myBestTimeSeconds,
        Double myAveragePace,


        List<CourseTopRunnerResponse> topRunners
) {
}
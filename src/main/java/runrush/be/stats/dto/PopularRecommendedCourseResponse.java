package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

public record PopularRecommendedCourseResponse(
        Long courseId,
        String courseTitle,
        String creatorName,
        double totalDistance,
        int totalCompletionCount,
        int uniqueRunnerCount,
        double averagePace
) {
    public static PopularRecommendedCourseResponse of(
            Long courseId,
            String courseTitle,
            String creatorName,
            double totalDistance,
            int totalCompletionCount,
            int uniqueRunnerCount,
            double averagePace
    ) {
        return new PopularRecommendedCourseResponse(
                courseId,
                courseTitle,
                creatorName,
                RoundUtil.round2(totalDistance),
                totalCompletionCount,
                uniqueRunnerCount,
                RoundUtil.round2(averagePace)
        );
    }
}
package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

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
    public static RecommendedCourseDetailStatsResponse of(
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
        return new RecommendedCourseDetailStatsResponse(
                courseId,
                courseTitle,
                creatorName,
                RoundUtil.round2(courseDistanceKm),
                totalCompletionCount,
                uniqueRunnerCount,
                RoundUtil.round2(averageCompletionTimeSeconds),
                RoundUtil.round2(averagePace),
                myCompletionCount,
                RoundUtil.round2Nullable(myBestTimeSeconds),
                RoundUtil.round2Nullable(myAveragePace),
                topRunners
        );
    }
}
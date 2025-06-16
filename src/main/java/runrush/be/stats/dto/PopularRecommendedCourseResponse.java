package runrush.be.stats.dto;

public record PopularRecommendedCourseResponse(
        Long courseId,
        String courseTitle,
        String creatorName,
        double totalDistance,
        int totalCompletionCount,
        int uniqueRunnerCount,
        double averagePace
) {
}
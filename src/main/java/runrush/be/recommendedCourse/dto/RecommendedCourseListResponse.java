package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseListResponse(
        Long courseId,
        String userName,
        String title,
        double totalDistance,
        double latitude,
        double longitude
) {
    public static RecommendedCourseListResponse toCourseListResponse(RecommendedCourse course) {
        return new RecommendedCourseListResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude()
        );
    }
}
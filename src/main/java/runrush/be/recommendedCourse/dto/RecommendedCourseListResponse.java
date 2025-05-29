package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseListResponse(
        Long id,
        String userName,
        String title,
        String endLocationName,
        double totalDistance,
        double latitude,
        double longitude
) {
    public static RecommendedCourseListResponse toCourseListResponse(RecommendedCourse course) {
        return new RecommendedCourseListResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getEndLocationName(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude()
        );
    }
}
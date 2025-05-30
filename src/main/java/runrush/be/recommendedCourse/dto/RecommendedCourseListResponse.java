package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseListResponse(
        Long id,
        String userName,
        String title,
        String imageUrl,
        String description,
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
                course.getImageUrl(),
                course.getDescription(),
                course.getEndLocationName(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude()
        );
    }
}
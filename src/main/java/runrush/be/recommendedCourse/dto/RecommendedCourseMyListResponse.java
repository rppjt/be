package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseMyListResponse(
        Long id,
        String userName,
        String title,
        String imageUrl,
        String description,
        String endLocationName,
        double totalDistance
) {
    public static RecommendedCourseMyListResponse toCourseListResponse(RecommendedCourse course) {
        return new RecommendedCourseMyListResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getImageUrl(),
                course.getDescription(),
                course.getEndLocationName(),
                course.getTotalDistance()
        );
    }
}
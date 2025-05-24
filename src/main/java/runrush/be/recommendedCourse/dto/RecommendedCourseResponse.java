package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseResponse(
        Long courseId,
        String userName,
        String title,
        String description,
        String pathGeoJson,
        double totalDistance,
        double latitude,
        double longitude
) {
    public static RecommendedCourseResponse toCourseResponse(RecommendedCourse course) {
        return new RecommendedCourseResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getDescription(),
                course.getPathGeoJson(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude()
        );
    }
}
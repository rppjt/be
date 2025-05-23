package runrush.be.recommendedCourse.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.user.domain.User;

public record RecommendedCourseResponse(
        Long courseId,
        String userName,
        RunningRecord runningRecord,
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
                course.getRunningRecord(),
                course.getTitle(),
                course.getDescription(),
                course.getPathGeoJson(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude()
        );
    }
}
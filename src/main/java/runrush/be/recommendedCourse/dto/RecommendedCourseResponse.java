package runrush.be.recommendedCourse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import runrush.be.common.util.GeoJsonUtil;
import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseResponse(
        Long id,
        String userName,
        String title,
        String description,
        String startLocationName,
        String endLocationName,
        JsonNode pathGeoJson,
        double totalDistance
) {
    public static RecommendedCourseResponse toCourseResponse(RecommendedCourse course) {
        return new RecommendedCourseResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getDescription(),
                course.getStartLocationName(),
                course.getEndLocationName(),
                GeoJsonUtil.parseGeoJson(course.getPathGeoJson()),
                course.getTotalDistance()
        );
    }
}
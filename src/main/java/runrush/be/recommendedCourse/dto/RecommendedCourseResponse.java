package runrush.be.recommendedCourse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import runrush.be.common.util.GeoJsonUtil;
import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseResponse(
        Long id,
        Long userId,
        String userName,
        String title,
        String description,
        String startLocationName,
        String endLocationName,
        JsonNode pathGeoJson,
        double totalDistance,
        long likeCount,
        boolean isLiked,
        boolean isBookmarked
) {
    public static RecommendedCourseResponse toCourseResponse(RecommendedCourse course,
                                                             long likeCount,
                                                             boolean isLiked,
                                                             boolean isBookmarked) {
        return new RecommendedCourseResponse(
                course.getId(),
                course.getUser().getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getDescription(),
                course.getStartLocationName(),
                course.getEndLocationName(),
                GeoJsonUtil.parseGeoJson(course.getPathGeoJson()),
                Math.round(course.getTotalDistance() * 100.0) / 100.0,
                likeCount,
                isLiked,
                isBookmarked
        );
    }
}
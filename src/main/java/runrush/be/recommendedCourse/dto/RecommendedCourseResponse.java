package runrush.be.recommendedCourse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import runrush.be.common.util.GeoJsonUtil;
import runrush.be.common.util.RoundUtil;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.stats.dto.CourseTopRunner;

import java.util.List;

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
        boolean isBookmarked,

        int totalCompletionCount,
        int uniqueRunnerCount,
        double averageCompletionTime,
        double averagePace,
        int myCompletionCount,
        Double myBestTime,
        Double myAveragePace,
        List<CourseTopRunner> topRunners
) {
    public static RecommendedCourseResponse toCourseResponse(RecommendedCourse course,
                                                             long likeCount,
                                                             boolean isLiked,
                                                             boolean isBookmarked,
                                                             int totalCompletionCount,
                                                             int uniqueRunnerCount,
                                                             double averageCompletionTime,
                                                             double averagePace,
                                                             int myCompletionCount,
                                                             Double myBestTime,
                                                             Double myAveragePace,
                                                             List<CourseTopRunner> topRunners) {
        return new RecommendedCourseResponse(
                course.getId(),
                course.getUser().getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getDescription(),
                course.getStartLocationName(),
                course.getEndLocationName(),
                GeoJsonUtil.parseGeoJson(course.getPathGeoJson()),
                RoundUtil.round2(course.getTotalDistance()),
                likeCount,
                isLiked,
                isBookmarked,
                totalCompletionCount,
                uniqueRunnerCount,
                averageCompletionTime,
                averagePace,
                myCompletionCount,
                myBestTime,
                myAveragePace,
                topRunners
        );
    }

    // 기존 호환성을 위한 오버로드 메소드
    public static RecommendedCourseResponse toCourseResponse(RecommendedCourse course,
                                                             long likeCount,
                                                             boolean isLiked,
                                                             boolean isBookmarked) {
        return toCourseResponse(course, likeCount, isLiked, isBookmarked,
                0, 0, 0.0, 0.0, 0, null, null, List.of());
    }
}
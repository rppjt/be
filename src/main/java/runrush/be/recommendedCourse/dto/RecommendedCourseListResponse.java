package runrush.be.recommendedCourse.dto;

import runrush.be.common.util.RoundUtil;
import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseListResponse(
        Long id,
        String userName,
        String title,
        String imageUrl,
        String description,
        String endLocationName,
        double totalDistance,
        boolean isBookmarked
) {
    public static RecommendedCourseListResponse toCourseListResponse(RecommendedCourse course, boolean isBookmarked) {
        return new RecommendedCourseListResponse(
                course.getId(),
                course.getUser().getName(),
                course.getTitle(),
                course.getImageUrl(),
                course.getDescription(),
                course.getEndLocationName(),
                RoundUtil.round2(course.getTotalDistance()),
                isBookmarked
        );
    }
}
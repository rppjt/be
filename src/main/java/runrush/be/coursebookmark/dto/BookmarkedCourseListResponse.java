package runrush.be.coursebookmark.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

import java.time.LocalDateTime;

public record BookmarkedCourseListResponse(
        Long bookmarkId,
        Long courseId,
        String imageUrl,
        String title,
        double totalDistance,
        String endLocationName,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkedCourseListResponse toBookmarkedCourseListResponse(RecommendedCourse course, Long bookmarkId) {
        return new BookmarkedCourseListResponse(
                bookmarkId,
                course.getId(),
                course.getImageUrl(),
                course.getTitle(),
                course.getTotalDistance(),
                course.getEndLocationName(),
                course.getCreatedAt()
        );
    }
}
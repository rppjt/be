package runrush.be.coursebookmark.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

import java.time.LocalDateTime;

public record BookmarkedCourseListResponse(
        Long bookmarkId,
        Long courseId,
        String courseTitle,
        double totalDistance,
        double endLatitude,
        double endLongitude,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkedCourseListResponse toBookmarkedCourseListResponse(RecommendedCourse course, Long bookmarkId) {
        return new BookmarkedCourseListResponse(
                bookmarkId,
                course.getId(),
                course.getTitle(),
                course.getTotalDistance(),
                course.getLatitude(),
                course.getLongitude(),
                course.getCreatedAt()
        );
    }
}
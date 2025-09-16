package runrush.be.runningrecord.dto;

import runrush.be.recommendedCourse.domain.RecommendedCourse;

public record RecommendedCourseInfo(
        Long id,
        String title
) {
    public static RecommendedCourseInfo from(RecommendedCourse course) {
        if (course == null) {
            return null;
        }

        return new RecommendedCourseInfo(
                course.getId(),
                course.getTitle()
        );
    }
}
package runrush.be.coursebookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import runrush.be.coursebookmark.domain.CourseBookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, Long> {
    Optional<CourseBookmark> findByUserIdAndRecommendedCourseId(Long userId, Long courseId);

    @Query("SELECT cb FROM CourseBookmark cb " +
            "JOIN FETCH cb.recommendedCourse " +
            "WHERE cb.user.id = :userId " +
            "ORDER BY cb.bookmarkedAt DESC")
    List<CourseBookmark> findWithCourseByUserId(Long userId);
}
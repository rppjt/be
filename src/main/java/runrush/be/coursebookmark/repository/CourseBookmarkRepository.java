package runrush.be.coursebookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.coursebookmark.domain.CourseBookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, Long> {
    Optional<CourseBookmark> findByUserIdAndCourseId(Long userId, Long courseId);
    List<CourseBookmark> findByUserId(Long userId);
}
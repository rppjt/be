package runrush.be.coursebookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import runrush.be.coursebookmark.domain.CourseBookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Optional<CourseBookmark> findByUserIdAndRecommendedCourseId(Long userId, Long courseId);
    
    // 락 없는 조회 (단순 확인용)
    @Query("SELECT cb FROM CourseBookmark cb WHERE cb.user.id = :userId AND cb.recommendedCourse.id = :courseId")
    Optional<CourseBookmark> findByUserIdAndRecommendedCourseIdWithoutLock(@Param("userId") Long userId, 
                                                                           @Param("courseId") Long courseId);

    @Query("SELECT cb FROM CourseBookmark cb " +
            "JOIN FETCH cb.recommendedCourse rc " +
            "JOIN FETCH rc.user " +
            "WHERE cb.user.id = :userId " +
            "ORDER BY cb.bookmarkedAt DESC")
    List<CourseBookmark> findWithCourseByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CourseBookmark cb WHERE cb.recommendedCourse.id = :courseId")
    void deleteByRecommendedCourseId(@Param("courseId") Long courseId);

    @Query("SELECT cb.recommendedCourse.id FROM CourseBookmark cb " +
            "WHERE cb.user.id = :userId")
    List<Long> findRecommendedCourseIdByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndRecommendedCourseId(Long userId, Long courseId);
}
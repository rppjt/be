package runrush.be.courselike.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.courselike.domain.CourseLike;

import java.util.Optional;

@Repository
public interface CourseLikeRepository extends JpaRepository<CourseLike, Long> {

    @Query("SELECT l FROM CourseLike l WHERE l.user.id = :userId AND l.recommendedCourse.id = :courseId")
    Optional<CourseLike> findByUserIdAndRecommendedCourseId(@Param("userId") Long userId,
                                                            @Param("courseId") Long courseId);

    long countByRecommendedCourseId(Long courseId);

    boolean existsByUserIdAndRecommendedCourseId(Long userId, Long courseId);
}
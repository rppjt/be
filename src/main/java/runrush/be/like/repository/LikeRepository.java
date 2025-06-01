package runrush.be.like.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.like.domain.Like;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.recommendedCourse.id = :corseId")
    Optional<Like> findByUserIdAndRecommendedCourseId(@Param("userId") Long userId,
                                                      @Param("courseId") Long courseId);
}
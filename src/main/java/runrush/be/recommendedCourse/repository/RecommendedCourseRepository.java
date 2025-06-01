package runrush.be.recommendedCourse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {
    boolean existsBySourceRecordId(Long sourceRecordId);

    @Query("SELECT rc FROM RecommendedCourse rc " +
            "JOIN FETCH rc.user " +
            "WHERE rc.user.id = :userId")
    List<RecommendedCourse> findWithUserByUserId(@Param("userId") Long userId);

    @Query("SELECT rc FROM RecommendedCourse rc " +
            "JOIN FETCH rc.user ")
    List<RecommendedCourse> findAllWithUser();

    @Query("SELECT rc FROM RecommendedCourse rc " +
            "JOIN FETCH rc.user " +
            "WHERE rc.id = :courseId")
    Optional<RecommendedCourse> findByCourseId(@Param("courseId") Long courseId);

    //좋아요순 정렬
    @Query("SELECT rc FROM RecommendedCourse rc " +
            "LEFT JOIN Like l ON l.recommendedCourse.id = rc.id " +
            "JOIN FETCH rc.user " +
            "GROUP BY rc.id " +
            "ORDER BY COUNT(l.id) DESC")
    List<RecommendedCourse> findAllOrderedByLikeCount();

    // 거리순 정렬
    @Query("SELECT rc FROM RecommendedCourse rc " +
            "JOIN FETCH rc.user " +
            "ORDER BY rc.totalDistance DESC")
    List<RecommendedCourse> findAllOrderedByTotalDistance();

    // 최신순 정렬
    @Query("SELECT rc FROM RecommendedCourse rc " +
            "JOIN FETCH rc.user " +
            "ORDER BY rc.createdAt DESC")
    List<RecommendedCourse> findAllOrderedByCreatedAt();
}
package runrush.be.recommendedCourse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;

import java.util.List;

@Repository
public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {
    boolean existsBySourceRecordId(Long sourceRecordId);

    List<RecommendedCourse> findByUserId(Long userId);
}
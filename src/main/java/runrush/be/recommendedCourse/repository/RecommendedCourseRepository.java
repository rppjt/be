package runrush.be.recommendedCourse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.runningrecord.domain.RunningRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {
    boolean existsByRunningRecord(RunningRecord runningRecord);
    Optional<RecommendedCourse> findByIdAndIsDeletedFalse(Long courseId);
    List<RecommendedCourse> findAllByIsDeletedFalse();
    List<RecommendedCourse> findByUserEmailAndIsDeletedFalse(String email);

}
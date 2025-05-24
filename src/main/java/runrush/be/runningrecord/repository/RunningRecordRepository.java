package runrush.be.runningrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.runningrecord.domain.RunningRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
    Optional<RunningRecord> findByIdAndIsDeletedFalse(Long id);
    List<RunningRecord> findByUserIdAndIsDeletedFalse(Long userId);
}
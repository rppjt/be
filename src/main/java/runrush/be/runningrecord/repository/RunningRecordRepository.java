package runrush.be.runningrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.runningrecord.domain.RunningRecord;

import java.util.List;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
    List<RunningRecord> findByUserEmail(String email);
}
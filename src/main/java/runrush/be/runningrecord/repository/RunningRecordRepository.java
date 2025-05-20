package runrush.be.runningrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import runrush.be.runningrecord.domain.RunningRecord;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
}

package runrush.be.runningrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.runningrecord.domain.RunningRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
    Optional<RunningRecord> findByIdAndIsDeletedFalse(Long id);

    List<RunningRecord> findByUserIdAndIsDeletedFalse(Long userId);

    @Query("SELECT rr FROM RunningRecord rr " +
            "WHERE rr.user.id = :userId " +
            "AND rr.isDeleted = false " +
            "AND rr.startedTime BETWEEN :startDate AND :endDate " +
            "ORDER BY rr.startedTime DESC")
    List<RunningRecord> findWeeklyRecords(@Param("userId") Long userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT rr FROM RunningRecord rr " +
            "WHERE rr.user.id = :userId " +
            "AND rr.isDeleted = false " +
            "AND YEAR(rr.startedTime) = :year " +
            "AND MONTH(rr.startedTime) = :month " +
            "ORDER BY rr.startedTime DESC")
    List<RunningRecord> findMonthlyRecords(@Param("userId") Long userId,
                                           @Param("year") int year,
                                           @Param("month") int month);
}
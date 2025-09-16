package runrush.be.runningrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.stats.dto.PopularRecommendedCourseResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
    @Query("SELECT rr FROM RunningRecord rr " +
            "JOIN FETCH rr.user " +
            "WHERE rr.id = :id AND rr.isDeleted = false")
    Optional<RunningRecord> findByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query("SELECT rr FROM RunningRecord rr " +
            "JOIN FETCH rr.user " +
            "LEFT JOIN FETCH rr.recommendedCourse rc " +
            "LEFT JOIN FETCH rc.user " +
            "WHERE rr.user.id = :userId AND rr.isDeleted = false " +
            "ORDER BY rr.startedTime DESC")
    List<RunningRecord> findByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

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

    Optional<RunningRecord> findByIdAndUserIdAndIsDeletedTrue(Long id, Long userId);

    List<RunningRecord> findByUserIdAndIsDeletedTrue(Long userId);

    /**
     * 특정 추천 코스 완주 기록들
     */
    @Query("SELECT rr FROM RunningRecord rr " +
            "JOIN FETCH rr.user " +
            "LEFT JOIN FETCH rr.recommendedCourse rc " +
            "LEFT JOIN FETCH rc.user " +
            "WHERE rr.recommendedCourse.id = :courseId AND rr.isDeleted = false " +
            "AND (rc IS NULL OR rc.isDeleted = false) " +
            "ORDER BY rr.totalTime ASC")
    List<RunningRecord> findByRecommendedCourseIdWithDetails(@Param("courseId") Long courseId);

    /**
     * 모든 추천 코스 기록들
     */
    @Query("SELECT rr FROM RunningRecord rr " +
            "LEFT JOIN FETCH rr.recommendedCourse rc " +
            "LEFT JOIN FETCH rc.user " +
            "LEFT JOIN FETCH rr.user " +
            "WHERE rr.recommendedCourse IS NOT NULL AND rr.isDeleted = false " +
            "AND (rc IS NULL OR rc.isDeleted = false) " +
            "ORDER BY rr.recommendedCourse.id")
    List<RunningRecord> findAllRecommendedCourseRecords();

    @Query("SELECT new runrush.be.stats.dto.PopularRecommendedCourseResponse(" +
            "rc.id, " +
            "rc.title, " +
            "rc.user.name, " +
            "rc.totalDistance / 1000.0, " +
            "CAST(COUNT(rr.id) AS int), " +
            "CAST(COUNT(DISTINCT rr.user.id) AS int), " +
            "COALESCE(AVG(rr.pace), 0.0)" +
            ") " +
            "FROM RunningRecord rr " +
            "JOIN rr.recommendedCourse rc " +
            "JOIN rc.user " +
            "WHERE rr.isDeleted = false AND rc.isDeleted = false " +
            "GROUP BY rc.id, rc.title, rc.user.name, rc.totalDistance " +
            "ORDER BY COUNT(rr.id) DESC")
    List<PopularRecommendedCourseResponse> findPopularRecommendedCourseStats();
    
    // 멱등성 키로 기존 기록 조회
    Optional<RunningRecord> findByIdempotencyKey(String idempotencyKey);
    
    // 멱등성 키 존재 여부 확인
    boolean existsByIdempotencyKey(String idempotencyKey);
}
package runrush.be.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.common.util.RoundUtil;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.stats.dto.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RunningStatsService {

    private final RunningRecordRepository runningRecordRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;

    private record BasicStats(
            int totalRuns,
            double totalDistance,
            long totalTime,
            double averagePace,
            double averageDistance
    ) {
    }

    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeeklyStats(Long userId, int weekOffset) {
        LocalDate today = LocalDate.now().plusWeeks(weekOffset);
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59, 59);

        List<RunningRecord> weeklyRecords = runningRecordRepository.findWeeklyRecords(userId, startDateTime, endDateTime);

        if (weeklyRecords.isEmpty()) {
            return WeeklyStatsResponse.empty(startOfWeek, endOfWeek);
        }

        return calculateWeeklyStats(weeklyRecords, startOfWeek, endOfWeek);
    }


    @Transactional(readOnly = true)
    public MonthlyStatsResponse getMonthlyStats(Long userId, Integer year, Integer month, Integer monthOffset) {
        LocalDate date;

        if (month != null && year != null) {
            if (month < 1 || month > 12) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            date = LocalDate.of(year, month, 1);
        } else {
            int offset = monthOffset != null ? monthOffset : 0;
            date = LocalDate.now().plusMonths(offset);
        }

        List<RunningRecord> monthlyRecords = runningRecordRepository.findMonthlyRecords(userId, date.getYear(), date.getMonthValue());

        if (monthlyRecords.isEmpty()) {
            return MonthlyStatsResponse.empty(date.getMonthValue(), date.getYear());
        }

        return calculateMonthlyStats(monthlyRecords, date.getYear(), date.getMonthValue());
    }

    @Transactional(readOnly = true)
    public PersonalBestStatsResponse getPersonalBestStats(Long userId) {
        List<RunningRecord> records = runningRecordRepository.findByUserIdAndIsDeletedFalse(userId);

        if (records.isEmpty()) {
            return PersonalBestStatsResponse.empty();
        }

        return calculatePersonalBestStats(records);
    }

    /**
     * 추천 코스의 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public RecommendedCourseDetailStats getRecommendedCourseDetailStats(Long courseId, Long userId) {
        RecommendedCourse course = recommendedCourseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));

        List<RunningRecord> courseRecords = runningRecordRepository.findByRecommendedCourseIdWithDetails(courseId);

        int totalCompletionCount = courseRecords.size();

        int uniqueRunnerCount = (int) courseRecords.stream()
                .map(record -> record.getUser().getId())
                .distinct()
                .count();

        double averageCompletionTime = courseRecords.stream()
                .mapToLong(RunningRecord::getTotalTime)
                .average()
                .orElse(0.0);

        double averagePace = courseRecords.stream()
                .mapToDouble(RunningRecord::getPace)
                .filter(pace -> pace > 0)
                .average()
                .orElse(0.0);

        List<RunningRecord> myRecords = courseRecords.stream()
                .filter(record -> record.getUser().getId().equals(userId))
                .toList();

        int myCompletionCount = myRecords.size();

        Double myBestTime = myRecords.stream()
                .map(RunningRecord::getTotalTime)
                .min(Long::compareTo)
                .map(Long::doubleValue)
                .orElse(null);

        Double myAveragePace = myRecords.stream()
                .mapToDouble(RunningRecord::getPace)
                .average()
                .orElse(Double.NaN);
        if (Double.isNaN(myAveragePace)) myAveragePace = null;

        // 상위 러너 TOP 5 계산
        List<CourseTopRunner> topRunners = courseRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getUser().getName(),
                        Collectors.minBy(Comparator.comparing(RunningRecord::getTotalTime))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(RunningRecord::getTotalTime))
                .limit(5)
                .map(record -> new CourseTopRunner(
                        record.getUser().getName(),
                        record.getTotalTime(),
                        record.getPace()
                ))
                .toList();

        return RecommendedCourseDetailStats.of(
                courseId,
                course.getTitle(),
                course.getUser().getName(),
                course.getTotalDistance() / 1000.0,
                totalCompletionCount,
                uniqueRunnerCount,
                averageCompletionTime,
                averagePace,
                myCompletionCount,
                myBestTime,
                myAveragePace,
                topRunners
        );
    }

    /**
     * 인기 추천 코스 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PopularRecommendedCourseResponse> getPopularRecommendedCourses() {
        return runningRecordRepository.findPopularRecommendedCourseStats()
                .stream()
                .limit(10)
                .toList();
    }

    private BasicStats calculateBasicStats(List<RunningRecord> records) {
        int totalRuns = records.size();

        double totalDistance = records.stream()
                .mapToDouble(RunningRecord::getTotalDistance)
                .sum() / 1000.0;

        long totalTime = records.stream()
                .mapToLong(RunningRecord::getTotalTime)
                .sum();

        double averagePace = records.stream()
                .mapToDouble(RunningRecord::getPace)
                .filter(pace -> pace > 0)
                .average()
                .orElse(0.0);

        double averageDistance = totalRuns > 0 ? totalDistance / totalRuns : 0.0;

        return new BasicStats(
                totalRuns,
                RoundUtil.round2(totalDistance),
                totalTime,
                RoundUtil.round2(averagePace),
                RoundUtil.round2(averageDistance)
        );
    }

    private WeeklyStatsResponse calculateWeeklyStats(List<RunningRecord> records, LocalDate startOfWeek, LocalDate endOfWeek) {
        BasicStats basic = calculateBasicStats(records);

        return  WeeklyStatsResponse.of(
                basic.totalRuns(),
                basic.totalDistance(),
                basic.totalTime(),
                basic.averagePace(),
                basic.averageDistance(),
                startOfWeek,
                endOfWeek
        );
    }

    private MonthlyStatsResponse calculateMonthlyStats(List<RunningRecord> records, int year, int month) {
        BasicStats basic = calculateBasicStats(records);

        int activeDays = (int) records.stream()
                .map(record -> record.getStartedTime().toLocalDate())
                .distinct()
                .count();

        double longestRun = records.stream()
                .mapToDouble(RunningRecord::getTotalDistance)
                .max()
                .orElse(0.0) / 1000.0;

        double fastestPace = records.stream()
                .mapToDouble(RunningRecord::getPace)
                .min()
                .orElse(0.0);

        return MonthlyStatsResponse.of(
                basic.totalRuns,
                basic.totalDistance,
                basic.totalTime,
                basic.averagePace,
                basic.averageDistance,
                activeDays,
                longestRun,
                fastestPace,
                month,
                year
        );
    }

    private PersonalBestStatsResponse calculatePersonalBestStats(List<RunningRecord> records) {
        BasicStats basic = calculateBasicStats(records);

        double longestDistance = records.stream()
                .mapToDouble(RunningRecord::getTotalDistance)
                .max().orElse(0.0) / 1000.0;

        double fastestPace = records.stream()
                .mapToDouble(RunningRecord::getPace)
                .filter(pace -> pace > 0)
                .min().orElse(0.0);

        long longestTime = records.stream()
                .mapToLong(RunningRecord::getTotalTime)
                .max().orElse(0L);

        PersonalBestStatsResponse.BestMonthRecord bestMonthRecord = calculateBestMonthRecord(records);

        return PersonalBestStatsResponse.of(
                longestDistance,
                fastestPace,
                longestTime,
                basic.totalDistance(),
                basic.totalRuns(),
                bestMonthRecord
        );
    }

    private PersonalBestStatsResponse.BestMonthRecord calculateBestMonthRecord(List<RunningRecord> records) {
        int currentYear = LocalDate.now().getYear();

        Map<Integer, Set<LocalDate>> monthlyDates = records.stream()
                .filter(record -> record.getStartedTime().getYear() == currentYear)
                .collect(Collectors.groupingBy(
                        record -> record.getStartedTime().getMonthValue(),
                        Collectors.mapping(
                                record -> record.getStartedTime().toLocalDate(),
                                Collectors.toSet()
                        )
                ));

        return monthlyDates.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparing(Set::size)))
                .map(entry -> new PersonalBestStatsResponse.BestMonthRecord(
                        entry.getKey(),
                        entry.getValue().size()
                ))
                .orElse(new PersonalBestStatsResponse.BestMonthRecord(0, 0));
    }
}
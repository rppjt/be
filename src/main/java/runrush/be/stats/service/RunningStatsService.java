package runrush.be.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.stats.dto.MonthlyStats;
import runrush.be.stats.dto.WeeklyStats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RunningStatsService {

    private final RunningRecordRepository runningRecordRepository;

    private record BasicStats(
            int totalRuns,
            double totalDistance,
            long totalTime,
            double averagePace,
            double averageDistance
    ) {
    }

    @Transactional(readOnly = true)
    public WeeklyStats getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59, 59);

        List<RunningRecord> weeklyRecords = runningRecordRepository.findWeeklyRecords(userId, startDateTime, endDateTime);

        if (weeklyRecords.isEmpty()) {
            return WeeklyStats.empty(startOfWeek, endOfWeek);
        }

        return calculateWeeklyStats(weeklyRecords, startOfWeek, endOfWeek);
    }

    @Transactional(readOnly = true)
    public MonthlyStats getMonthlyStats(Long userId, int month, int year) {
        List<RunningRecord> monthlyRecords = runningRecordRepository.findMonthlyRecords(userId, year, month);

        if (monthlyRecords.isEmpty()) {
            return MonthlyStats.empty(month, year);
        }

        return calculateMonthlyStats(monthlyRecords, month, year);
    }

    @Transactional(readOnly = true)
    public MonthlyStats getCurrentMonthlyStats(Long userId) {
        LocalDate today = LocalDate.now();
        return getMonthlyStats(userId, today.getYear(), today.getMonthValue());
    }

    @Transactional(readOnly = true)
    public MonthlyStats getLastMonthlyStats(Long userId) {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        return getMonthlyStats(userId, lastMonth.getYear(), lastMonth.getMonthValue());
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
                Math.round(totalDistance * 100.0) / 100.0,
                totalTime,
                Math.round(averagePace * 100.0) / 100.0,
                Math.round(averageDistance * 100.0) / 100.0);
    }

    private WeeklyStats calculateWeeklyStats(List<RunningRecord> records, LocalDate startOfWeek, LocalDate endOfWeek) {
        BasicStats basic = calculateBasicStats(records);

        return new WeeklyStats(
                basic.totalRuns(),
                basic.totalDistance(),
                basic.totalTime(),
                basic.averagePace(),
                basic.averageDistance(),
                startOfWeek,
                endOfWeek
        );
    }

    private MonthlyStats calculateMonthlyStats(List<RunningRecord> records, int month, int year) {
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

        return new MonthlyStats(
                basic.totalRuns,
                basic.totalDistance,
                basic.totalTime,
                basic.averagePace,
                basic.averageDistance,
                activeDays,
                Math.round(longestRun * 100.0) / 100.0,
                Math.round(fastestPace * 100.0) / 100.0,
                month,
                year
        );
    }
}
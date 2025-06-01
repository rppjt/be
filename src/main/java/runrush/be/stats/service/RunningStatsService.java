package runrush.be.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
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

    private WeeklyStats calculateWeeklyStats(List<RunningRecord> records, LocalDate startOfWeek, LocalDate endOfWeek) {
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

        return new WeeklyStats(
                totalRuns,
                Math.round(totalDistance * 100.0) / 100.0,
                totalTime,
                Math.round(averagePace * 100.0) / 100.0,
                Math.round(averageDistance * 100.0) / 100.0,
                startOfWeek,
                endOfWeek
        );
    }
}
package runrush.be.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.stats.dto.MonthlyStats;
import runrush.be.stats.dto.PersonalBestStats;
import runrush.be.stats.dto.WeeklyStats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public WeeklyStats getWeeklyStats(Long userId, int weekOffset) {
        LocalDate today = LocalDate.now().plusWeeks(weekOffset);
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
    public MonthlyStats getMonthlyStats(Long userId, Integer year, Integer month, Integer monthOffset) {
        LocalDate date;

        if (month != null && year != null) {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("월은 1-12 사이의 값이어야 합니다. 입력값: " + month);
            }
            date = LocalDate.of(year, month, 1);
        } else {
            int offset = monthOffset != null ? monthOffset : 0;
            date = LocalDate.now().plusMonths(offset);
        }

        List<RunningRecord> monthlyRecords = runningRecordRepository.findMonthlyRecords(userId, date.getYear(), date.getMonthValue());

        if (monthlyRecords.isEmpty()) {
            return MonthlyStats.empty(date.getMonthValue(), date.getYear());
        }

        return calculateMonthlyStats(monthlyRecords, date.getYear(), date.getMonthValue());
    }

    @Transactional(readOnly = true)
    public PersonalBestStats getPersonalBestStats(Long userId) {
        List<RunningRecord> records = runningRecordRepository.findByUserIdAndIsDeletedFalse(userId);

        if (records.isEmpty()) {
            return PersonalBestStats.empty();
        }

        return calculatePersonalBestStats(records);
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

    private MonthlyStats calculateMonthlyStats(List<RunningRecord> records, int year, int month) {
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

    private PersonalBestStats calculatePersonalBestStats(List<RunningRecord> records) {
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

        PersonalBestStats.BestMonthRecord bestMonthRecord = calculateBestMonthRecord(records);

        return new PersonalBestStats(
                Math.round(longestDistance * 100.0) / 100.0,
                Math.round(fastestPace * 100.0) / 100.0,
                longestTime,
                basic.totalDistance(),
                basic.totalRuns(),
                bestMonthRecord
        );
    }

    private PersonalBestStats.BestMonthRecord calculateBestMonthRecord(List<RunningRecord> records) {
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
                .map(entry -> new PersonalBestStats.BestMonthRecord(
                        entry.getKey(),
                        entry.getValue().size()
                ))
                .orElse(new PersonalBestStats.BestMonthRecord(0, 0));
    }
}
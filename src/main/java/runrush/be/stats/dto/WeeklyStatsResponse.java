package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

import java.time.LocalDate;

public record WeeklyStatsResponse(
        int totalRuns,
        double totalDistance,
        long totalTime,
        double averagePace,
        double averageDistance,
        LocalDate startOfWeek,
        LocalDate endOfWeek
) {
    public static WeeklyStatsResponse of(
            int totalRuns,
            double totalDistance,
            long totalTime,
            double averagePace,
            double averageDistance,
            LocalDate startOfWeek,
            LocalDate endOfWeek
    ) {
        return new WeeklyStatsResponse(
                totalRuns,
                RoundUtil.round2(totalDistance),
                totalTime,
                RoundUtil.round2(averagePace),
                RoundUtil.round2(averageDistance),
                startOfWeek,
                endOfWeek
        );
    }

    public static WeeklyStatsResponse empty(LocalDate start, LocalDate end) {
        return new WeeklyStatsResponse(0, 0, 0, 0, 0, start, end);
    }
}
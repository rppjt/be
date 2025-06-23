package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

public record MonthlyStatsResponse(
        int totalRuns,
        double totalDistance,
        long totalTime,
        double averagePace,
        double averageDistance,

        int activeDays,
        double longestRun,
        double fastestPace,

        int month,
        int year
) {
    public static MonthlyStatsResponse of(
            int totalRuns,
            double totalDistance,
            long totalTime,
            double averagePace,
            double averageDistance,
            int activeDays,
            double longestRun,
            double fastestPace,
            int month,
            int year
    ) {
        return new MonthlyStatsResponse(
                totalRuns,
                RoundUtil.round2(totalDistance),
                totalTime,
                RoundUtil.round2(averagePace),
                RoundUtil.round2(averageDistance),
                activeDays,
                RoundUtil.round2(longestRun),
                RoundUtil.round2(fastestPace),
                month,
                year
        );
    }

    public static MonthlyStatsResponse empty(int month, int year) {
        return new MonthlyStatsResponse(0, 0.0, 0L, 0.0, 0.0, 0, 0.0, 0.0, month, year);
    }
}
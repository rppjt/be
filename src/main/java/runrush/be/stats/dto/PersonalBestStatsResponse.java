package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

public record PersonalBestStatsResponse(
        double longestDistance,
        double fastestPace,
        long longestTime,
        double totalDistance,
        int totalRuns,
        BestMonthRecord bestMonthRecord

) {
    public record BestMonthRecord(
            int month,
            int activeDays
    ) {
    }

    public static PersonalBestStatsResponse of(
            double longestDistance,
            double fastestPace,
            long longestTime,
            double totalDistance,
            int totalRuns,
            BestMonthRecord bestMonthRecord
    ) {
        return new PersonalBestStatsResponse(
                RoundUtil.round2(longestDistance),
                RoundUtil.round2(fastestPace),
                longestTime,
                RoundUtil.round2(totalDistance),
                totalRuns,
                bestMonthRecord
        );
    }

    public static PersonalBestStatsResponse empty() {
        return new PersonalBestStatsResponse(
                0.0,
                0.0,
                0L,
                0.0,
                0,
                new BestMonthRecord(0, 0)
        );
    }
}
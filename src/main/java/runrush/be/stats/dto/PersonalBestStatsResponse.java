package runrush.be.stats.dto;

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
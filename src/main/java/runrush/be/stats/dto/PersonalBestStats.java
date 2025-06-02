package runrush.be.stats.dto;

public record PersonalBestStats(
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


    public static PersonalBestStats empty() {
        return new PersonalBestStats(
                0.0,
                0.0,
                0L,
                0.0,
                0,
                new BestMonthRecord(0, 0)
        );
    }
}
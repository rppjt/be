package runrush.be.stats.dto;

public record CourseTopRunnerResponse(
        String runnerName,
        long bestCompletionTimeSeconds,
        double bestPace
) {
}
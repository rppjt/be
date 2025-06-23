package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

public record CourseTopRunnerResponse(
        String runnerName,
        long bestCompletionTimeSeconds,
        double bestPace
) {
    public static CourseTopRunnerResponse of(
            String runnerName,
            long bestCompletionTimeSeconds,
            double bestPace
    ) {
        return new CourseTopRunnerResponse(
                runnerName,
                bestCompletionTimeSeconds,
                RoundUtil.round2(bestPace)
        );
    }
}
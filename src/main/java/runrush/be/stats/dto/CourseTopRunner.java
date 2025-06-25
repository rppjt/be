package runrush.be.stats.dto;

import runrush.be.common.util.RoundUtil;

public record CourseTopRunner(
        String runnerName,
        long bestCompletionTimeSeconds,
        double bestPace
) {
    public static CourseTopRunner of(
            String runnerName,
            long bestCompletionTimeSeconds,
            double bestPace
    ) {
        return new CourseTopRunner(
                runnerName,
                bestCompletionTimeSeconds,
                RoundUtil.round2(bestPace)
        );
    }
}
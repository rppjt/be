package runrush.be.common.util;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RoundUtil {
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static Double round2Nullable(Double value) {
        return value == null ? null : round2(value);
    }
}
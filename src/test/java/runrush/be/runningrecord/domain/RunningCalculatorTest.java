package runrush.be.runningrecord.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class RunningCalculatorTest {

    private final RunningCalculator calculator = new RunningCalculator();

    @Test
    @DisplayName("거리 계산 - 유효한 LineString GeoJSON")
    void calculateTotalDistance_ValidLineString() {
        String validGeoJson = """
                {
                    "type": "LineString",
                    "coordinates": [
                        [126.970, 37.554],
                        [126.982, 37.563]
                    ]
                }
                """;

        double distance = calculator.calculateTotalDistance(validGeoJson);

        assertThat(distance).isGreaterThan(0);
        assertThat(distance).isLessThan(5000);
    }

    @Test
    @DisplayName("거리 계산 - 여러 지점이 있는 LineString")
    void calculateTotalDistance_MultiplePoints() {
        String multiPointGeoJson = """
                {
                    "type": "LineString",
                    "coordinates": [
                        [126.970, 37.554],
                        [126.975, 37.558],
                        [126.982, 37.563]
                    ]
                }
                """;

        double distance = calculator.calculateTotalDistance(multiPointGeoJson);

        assertThat(distance).isGreaterThan(0);
    }

    @Test
    @DisplayName("거리 계산 - 잘못된 GeoJSON 타입이면 예외")
    void calculateTotalDistance_InvalidType_ThrowsException() {
        String invalidTypeGeoJson = """
                {
                    "type": "Point",
                    "coordinates": [126.970, 37.554]
                }
                """;

        assertThatThrownBy(() -> calculator.calculateTotalDistance(invalidTypeGeoJson))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);
    }

    @Test
    @DisplayName("거리 계산 - 좌표가 2개 미만이면 예외")
    void calculateTotalDistance_InsufficientCoordinates_ThrowsException() {
        String invalidGeoJson = """
                {
                    "type": "LineString",
                    "coordinates": [
                        [126.970, 37.554]
                    ]
                }
                """;

        assertThatThrownBy(() -> calculator.calculateTotalDistance(invalidGeoJson))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);
    }

    @Test
    @DisplayName("거리 계산 - 잘못된 JSON 형식이면 예외")
    void calculateTotalDistance_InvalidJson_ThrowsException() {
        String invalidJson = "{ invalid json format }";

        assertThatThrownBy(() -> calculator.calculateTotalDistance(invalidJson))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);
    }

    @Test
    @DisplayName("경험치 계산 - 10미터당 1경험치")
    void calculateExperiencePoints_StandardRate() {
        assertThat(calculator.calculateExperiencePoints(100.0)).isEqualTo(10);
        assertThat(calculator.calculateExperiencePoints(50.0)).isEqualTo(5);
        assertThat(calculator.calculateExperiencePoints(1000.0)).isEqualTo(100);
    }

    @Test
    @DisplayName("경험치 계산 - 10미터 미만은 경험치 없음")
    void calculateExperiencePoints_LessThan10Meters() {
        assertThat(calculator.calculateExperiencePoints(9.9)).isEqualTo(0);
        assertThat(calculator.calculateExperiencePoints(5.0)).isEqualTo(0);
    }

    @Test
    @DisplayName("경험치 계산 - 0 거리는 0 경험치")
    void calculateExperiencePoints_ZeroDistance() {
        assertThat(calculator.calculateExperiencePoints(0.0)).isEqualTo(0);
    }

    @Test
    @DisplayName("페이스 계산 - 1km를 10분(600초)에 뛰면 10분/km")
    void calculatePace_Standard() {
        double distance = 1000.0;
        long time = 600L;

        double pace = calculator.calculatePace(distance, time);

        assertThat(pace).isEqualTo(10.0); // 10분/km
    }

    @Test
    @DisplayName("페이스 계산 - 2km를 12분에 뛰면 6분/km")
    void calculatePace_FastPace() {
        double distance = 2000.0;
        long time = 720L;

        double pace = calculator.calculatePace(distance, time);

        assertThat(pace).isEqualTo(6.0); // 6분/km
    }

    @Test
    @DisplayName("페이스 계산 - 거리가 0 이하면 예외")
    void calculatePace_ZeroDistance_ThrowsException() {
        assertThatThrownBy(() -> calculator.calculatePace(0.0, 600L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);

        assertThatThrownBy(() -> calculator.calculatePace(-100.0, 600L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);
    }

    @Test
    @DisplayName("러닝 시간 검증 - 정상적인 시간")
    void validateRunningTime_ValidTime() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 30, 0);

        assertThatCode(() -> calculator.validateRunningTime(startTime, endTime))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("러닝 시간 검증 - 시작 시간이 종료 시간보다 늦으면 예외")
    void validateRunningTime_StartAfterEnd_ThrowsException() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 30, 0);

        assertThatThrownBy(() -> calculator.validateRunningTime(startTime, endTime))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RUNNING_TIME);
    }

    @Test
    @DisplayName("러닝 시간 검증 - 시작과 종료가 같으면 예외")
    void validateRunningTime_SameTime_ThrowsException() {
        LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);

        assertThatThrownBy(() -> calculator.validateRunningTime(sameTime, sameTime))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RUNNING_TIME);
    }

    @Test
    @DisplayName("총 러닝 시간 계산 - 초 단위 반환")
    void calculateTotalTimeInSeconds() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 30, 0);

        long totalTime = calculator.calculateTotalTimeInSeconds(startTime, endTime);

        assertThat(totalTime).isEqualTo(1800L); // 30분 = 1800초
    }

    @Test
    @DisplayName("총 러닝 시간 계산 - 잘못된 시간이면 예외")
    void calculateTotalTimeInSeconds_InvalidTime_ThrowsException() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 30, 0);

        assertThatThrownBy(() -> calculator.calculateTotalTimeInSeconds(startTime, endTime))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RUNNING_TIME);
    }

    @Test
    @DisplayName("페이스 계산 - 소수점 둘째 자리까지 반올림")
    void calculatePace_RoundingTest() {
        double distance = 1337.0;
        long time = 402L;

        double pace = calculator.calculatePace(distance, time);

        assertThat(pace).isNotNull();
        assertThat(pace).isEqualTo(Math.round(pace * 100.0) / 100.0);
    }
}
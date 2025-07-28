package runrush.be.runningrecord.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunningRecordFactoryTest {

    @Mock
    private RunningCalculator runningCalculator;
    
    @Mock
    private LocationResolver locationResolver;
    
    @InjectMocks
    private RunningRecordFactory factory;

    @Test
    @DisplayName("러닝 기록 생성 성공 - 기본 케이스")
    void createRunningRecord_Success() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        RecommendedCourse course = null; // 추천 코스 없음
        String imageUrl = "https://example.com/image.jpg";

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(anyString())).thenReturn(1500.0);
        when(runningCalculator.calculateTotalTimeInSeconds(any(), any())).thenReturn(900L);
        when(runningCalculator.calculatePace(1500.0, 900L)).thenReturn(10.0);
        when(locationResolver.resolveLocationName(37.566, 126.977)).thenReturn("서울역");
        when(locationResolver.resolveLocationName(37.563, 126.982)).thenReturn("명동");

        RunningRecord result = factory.createRunningRecord(request, user, course, imageUrl);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getRecommendedCourse()).isNull();
        assertThat(result.getImageUrl()).isEqualTo(imageUrl);
        assertThat(result.getTotalDistance()).isEqualTo(1500.0);
        assertThat(result.getTotalTime()).isEqualTo(900L);
        assertThat(result.getPace()).isEqualTo(10.0);
        assertThat(result.getStartLocationName()).isEqualTo("서울역");
        assertThat(result.getEndLocationName()).isEqualTo("명동");

        verify(runningCalculator).validateRunningTime(request.startedTime(), request.endedTime());
        verify(runningCalculator).calculateTotalDistance(request.pathGeoJson());
        verify(runningCalculator).calculateTotalTimeInSeconds(request.startedTime(), request.endedTime());
        verify(runningCalculator).calculatePace(1500.0, 900L);
        verify(locationResolver, times(2)).resolveLocationName(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("러닝 기록 생성 성공 - 추천 코스 포함")
    void createRunningRecord_WithRecommendedCourse() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        RecommendedCourse course = createTestRecommendedCourse();
        String imageUrl = "https://example.com/image.jpg";

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(anyString())).thenReturn(2000.0);
        when(runningCalculator.calculateTotalTimeInSeconds(any(), any())).thenReturn(1200L);
        when(runningCalculator.calculatePace(2000.0, 1200L)).thenReturn(10.0);
        when(locationResolver.resolveLocationName(anyDouble(), anyDouble())).thenReturn("테스트 위치");

        RunningRecord result = factory.createRunningRecord(request, user, course, imageUrl);

        assertThat(result.getRecommendedCourse()).isEqualTo(course);
        assertThat(result.getTotalDistance()).isEqualTo(2000.0);
        assertThat(result.getTotalTime()).isEqualTo(1200L);
    }

    @Test
    @DisplayName("러닝 기록 생성 실패 - 잘못된 러닝 시간")
    void createRunningRecord_InvalidRunningTime_ThrowsException() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        String imageUrl = "https://example.com/image.jpg";

        doThrow(new BusinessException(ErrorCode.INVALID_RUNNING_TIME))
                .when(runningCalculator).validateRunningTime(any(), any());

        assertThatThrownBy(() -> factory.createRunningRecord(request, user, null, imageUrl))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RUNNING_TIME);

        verify(runningCalculator, never()).calculateTotalDistance(anyString());
        verify(runningCalculator, never()).calculateTotalTimeInSeconds(any(), any());
        verify(locationResolver, never()).resolveLocationName(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("러닝 기록 생성 실패 - 잘못된 GeoJSON")
    void createRunningRecord_InvalidGeoJson_ThrowsException() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        String imageUrl = "https://example.com/image.jpg";

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(anyString()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PATH_DATA));

        assertThatThrownBy(() -> factory.createRunningRecord(request, user, null, imageUrl))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PATH_DATA);
    }

    @Test
    @DisplayName("러닝 기록 생성 - 위치 조회 실패 시에도 진행")
    void createRunningRecord_LocationResolveFailure() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        String imageUrl = "https://example.com/image.jpg";

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(anyString())).thenReturn(1000.0);
        when(runningCalculator.calculateTotalTimeInSeconds(any(), any())).thenReturn(600L);
        when(runningCalculator.calculatePace(1000.0, 600L)).thenReturn(10.0);
        
        when(locationResolver.resolveLocationName(anyDouble(), anyDouble()))
                .thenReturn("Unknown Location");

        RunningRecord result = factory.createRunningRecord(request, user, null, imageUrl);

        assertThat(result).isNotNull();
        assertThat(result.getStartLocationName()).isEqualTo("Unknown Location");
        assertThat(result.getEndLocationName()).isEqualTo("Unknown Location");
    }

    @Test
    @DisplayName("러닝 기록 생성 - null 이미지 URL 처리")
    void createRunningRecord_NullImageUrl() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        String imageUrl = null; // null 이미지 URL

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(anyString())).thenReturn(800.0);
        when(runningCalculator.calculateTotalTimeInSeconds(any(), any())).thenReturn(480L);
        when(runningCalculator.calculatePace(800.0, 480L)).thenReturn(10.0);
        when(locationResolver.resolveLocationName(anyDouble(), anyDouble())).thenReturn("테스트");

        RunningRecord result = factory.createRunningRecord(request, user, null, imageUrl);

        assertThat(result.getImageUrl()).isNull();
        assertThat(result.getTotalDistance()).isEqualTo(800.0);
    }

    @Test
    @DisplayName("러닝 기록 생성 - 모든 계산 결과 정확성 확인")
    void createRunningRecord_AllCalculationsAccuracy() {
        RunningRecordRequest request = createTestRequest();
        User user = createTestUser();
        String imageUrl = "test.jpg";

        doNothing().when(runningCalculator).validateRunningTime(any(), any());
        when(runningCalculator.calculateTotalDistance(request.pathGeoJson())).thenReturn(5000.0);
        when(runningCalculator.calculateTotalTimeInSeconds(request.startedTime(), request.endedTime())).thenReturn(1800L);
        when(runningCalculator.calculatePace(5000.0, 1800L)).thenReturn(6.0);
        when(locationResolver.resolveLocationName(request.startLatitude(), request.startLongitude())).thenReturn("시작점");
        when(locationResolver.resolveLocationName(request.endLatitude(), request.endLongitude())).thenReturn("도착점");

        RunningRecord result = factory.createRunningRecord(request, user, null, imageUrl);

        assertThat(result.getPathGeoJson()).isEqualTo(request.pathGeoJson());
        assertThat(result.getStartLatitude()).isEqualTo(request.startLatitude());
        assertThat(result.getStartLongitude()).isEqualTo(request.startLongitude());
        assertThat(result.getEndLatitude()).isEqualTo(request.endLatitude());
        assertThat(result.getEndLongitude()).isEqualTo(request.endLongitude());
        assertThat(result.getStartedTime()).isEqualTo(request.startedTime());
        assertThat(result.getEndedTime()).isEqualTo(request.endedTime());
        assertThat(result.getTotalDistance()).isEqualTo(5000.0);
        assertThat(result.getTotalTime()).isEqualTo(1800L);
        assertThat(result.getPace()).isEqualTo(6.0);
        assertThat(result.getStartLocationName()).isEqualTo("시작점");
        assertThat(result.getEndLocationName()).isEqualTo("도착점");
    }

    private RunningRecordRequest createTestRequest() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 9, 30, 0);
        
        return new RunningRecordRequest(
                """
                {
                    "type": "LineString",
                    "coordinates": [[126.977, 37.566], [126.982, 37.563]]
                }
                """, // pathGeoJson
                37.566, // startLatitude
                126.977, // startLongitude
                37.563, // endLatitude
                126.982, // endLongitude
                startTime,
                endTime,
                1L // recommendedCourseId
        );
    }

    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }

    private RecommendedCourse createTestRecommendedCourse() {
        return RecommendedCourse.builder()
                .user(createTestUser())
                .title("테스트 추천 코스")
                .description("테스트용 코스입니다")
                .totalDistance(2000.0)
                .build();
    }
}
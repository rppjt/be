package runrush.be.stats.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.stats.dto.*;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunningStatsServiceTest {

    @Mock
    private RunningRecordRepository runningRecordRepository;
    
    @Mock
    private RecommendedCourseRepository recommendedCourseRepository;
    
    @InjectMocks
    private RunningStatsService runningStatsService;

    @Test
    @DisplayName("주간 통계 조회 - 빈 데이터")
    void getWeeklyStats_EmptyData() {
        Long userId = 1L;
        int weekOffset = 0;
        
        when(runningRecordRepository.findWeeklyRecords(eq(userId), any(), any()))
                .thenReturn(List.of());

        WeeklyStatsResponse result = runningStatsService.getWeeklyStats(userId, weekOffset);

        assertThat(result).isNotNull();
        assertThat(result.totalRuns()).isEqualTo(0);
        assertThat(result.totalDistance()).isEqualTo(0.0);
        assertThat(result.totalTime()).isEqualTo(0L);
    }

    @Test
    @DisplayName("월간 통계 조회 - 잘못된 월")
    void getMonthlyStats_InvalidMonth() {
        Long userId = 1L;
        Integer year = 2024;
        Integer month = 13;

        assertThatThrownBy(() -> runningStatsService.getMonthlyStats(userId, year, month, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("월간 통계 조회 - 0월")
    void getMonthlyStats_ZeroMonth() {
        Long userId = 1L;
        Integer year = 2024;
        Integer month = 0;

        assertThatThrownBy(() -> runningStatsService.getMonthlyStats(userId, year, month, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("월간 통계 조회 - 빈 데이터")
    void getMonthlyStats_EmptyData() {
        Long userId = 1L;
        Integer year = 2024;
        Integer month = 1;
        
        when(runningRecordRepository.findMonthlyRecords(userId, year, month))
                .thenReturn(List.of());

        MonthlyStatsResponse result = runningStatsService.getMonthlyStats(userId, year, month, null);

        assertThat(result).isNotNull();
        assertThat(result.totalRuns()).isEqualTo(0);
        assertThat(result.totalDistance()).isEqualTo(0.0);
        assertThat(result.month()).isEqualTo(1);
        assertThat(result.year()).isEqualTo(2024);
    }

    @Test
    @DisplayName("개인 기록 통계 조회 - 빈 데이터")
    void getPersonalBestStats_EmptyData() {
        Long userId = 1L;
        
        when(runningRecordRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of());

        PersonalBestStatsResponse result = runningStatsService.getPersonalBestStats(userId);

        assertThat(result).isNotNull();
        assertThat(result.longestDistance()).isEqualTo(0.0);
        assertThat(result.fastestPace()).isEqualTo(0.0);
        assertThat(result.longestTime()).isEqualTo(0L);
        assertThat(result.totalDistance()).isEqualTo(0.0);
        assertThat(result.totalRuns()).isEqualTo(0);
    }

    @Test
    @DisplayName("추천 코스 상세 통계 조회 실패 - 코스 없음")
    void getRecommendedCourseDetailStats_CourseNotFound() {
        Long courseId = 999L;
        Long userId = 1L;
        
        when(recommendedCourseRepository.findByCourseId(courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningStatsService.getRecommendedCourseDetailStats(courseId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);
        
        verify(runningRecordRepository, never()).findByRecommendedCourseIdWithDetails(anyLong());
    }

    @Test
    @DisplayName("인기 추천 코스 목록 조회 - 빈 데이터")
    void getPopularRecommendedCourses_EmptyData() {
        when(runningRecordRepository.findAllRecommendedCourseRecords())
                .thenReturn(List.of());
        when(recommendedCourseRepository.findAllWithUser())
                .thenReturn(List.of());

        List<PopularRecommendedCourseResponse> result = runningStatsService.getPopularRecommendedCourses();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주간 통계 조회 - 음수 오프셋")
    void getWeeklyStats_NegativeOffset() {
        Long userId = 1L;
        int weekOffset = -2;
        
        when(runningRecordRepository.findWeeklyRecords(eq(userId), any(), any()))
                .thenReturn(List.of());

        WeeklyStatsResponse result = runningStatsService.getWeeklyStats(userId, weekOffset);

        assertThat(result).isNotNull();
        verify(runningRecordRepository).findWeeklyRecords(eq(userId), any(), any());
    }

    @Test
    @DisplayName("월간 통계 조회 - 월 오프셋 사용")
    void getMonthlyStats_WithMonthOffset() {
        Long userId = 1L;
        Integer monthOffset = -1;
        
        when(runningRecordRepository.findMonthlyRecords(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of());

        MonthlyStatsResponse result = runningStatsService.getMonthlyStats(userId, null, null, monthOffset);

        assertThat(result).isNotNull();
        verify(runningRecordRepository).findMonthlyRecords(eq(userId), anyInt(), anyInt());
    }

    @Test
    @DisplayName("월간 통계 조회 - 양수 오프셋")
    void getMonthlyStats_PositiveOffset() {
        Long userId = 1L;
        Integer monthOffset = 2;
        
        when(runningRecordRepository.findMonthlyRecords(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of());

        MonthlyStatsResponse result = runningStatsService.getMonthlyStats(userId, null, null, monthOffset);

        assertThat(result).isNotNull();
        verify(runningRecordRepository).findMonthlyRecords(eq(userId), anyInt(), anyInt());
    }

    @Test
    @DisplayName("월간 통계 조회 - 기본값 (현재 월)")
    void getMonthlyStats_DefaultCurrentMonth() {
        Long userId = 1L;
        
        when(runningRecordRepository.findMonthlyRecords(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of());

        MonthlyStatsResponse result = runningStatsService.getMonthlyStats(userId, null, null, null);

        assertThat(result).isNotNull();
        verify(runningRecordRepository).findMonthlyRecords(eq(userId), anyInt(), anyInt());
    }

    @Test
    @DisplayName("주간 통계 조회 - 양수 오프셋")
    void getWeeklyStats_PositiveOffset() {
        Long userId = 1L;
        int weekOffset = 1; // 다음주
        
        when(runningRecordRepository.findWeeklyRecords(eq(userId), any(), any()))
                .thenReturn(List.of());

        WeeklyStatsResponse result = runningStatsService.getWeeklyStats(userId, weekOffset);

        assertThat(result).isNotNull();
        verify(runningRecordRepository).findWeeklyRecords(eq(userId), any(), any());
    }

    @Test
    @DisplayName("통계 서비스 리포지토리 의존성 검증")
    void verifyRepositoryDependencies() {
        assertThat(runningRecordRepository).isNotNull();
        assertThat(recommendedCourseRepository).isNotNull();
        assertThat(runningStatsService).isNotNull();
    }
    
    @Test
    @DisplayName("주간 통계 조회 - 실제 데이터")
    void getWeeklyStats_WithData() {
        // TODO: 구현 필요
        Long userId = 1L;
        int weekOffset = 0;

        List<RunningRecord> weeklyRecords = List.of(
                createTestRunningRecord(1L, 1500.0, 1800L, 8.0),
                createTestRunningRecord(2L, 2000.0, 2400L, 8.5)
                );

        when(runningRecordRepository.findWeeklyRecords(eq(userId), any(), any()))
                .thenReturn(weeklyRecords);

        WeeklyStatsResponse result = runningStatsService.getWeeklyStats(userId, weekOffset);

        assertThat(result).isNotNull();
        assertThat(result.totalRuns()).isEqualTo(2);
        assertThat(result.totalDistance()).isEqualTo(3.5); // 3500m -> 3.5km
        assertThat(result.totalTime()).isEqualTo(4200L);
    }
    
    @Test
    @DisplayName("월간 통계 조회 - 실제 데이터")
    void getMonthlyStats_WithData() {
        Long userId = 1L;
        Integer year = 2024;
        Integer month = 1;
        
        List<RunningRecord> monthlyRecords = List.of(
            createTestRunningRecord(1L, 1500.0, 1800L, 8.0),
            createTestRunningRecord(2L, 2000.0, 2400L, 8.5),
            createTestRunningRecord(3L, 1800.0, 2160L, 8.2)
        );
        
        when(runningRecordRepository.findMonthlyRecords(userId, year, month))
                .thenReturn(monthlyRecords);

        MonthlyStatsResponse result = runningStatsService.getMonthlyStats(userId, year, month, null);

        assertThat(result).isNotNull();
        assertThat(result.totalRuns()).isEqualTo(3);
        assertThat(result.totalDistance()).isEqualTo(5.3); // 5300m -> 5.3km
        assertThat(result.totalTime()).isEqualTo(6360L);
        assertThat(result.month()).isEqualTo(1);
        assertThat(result.year()).isEqualTo(2024);
    }
    
    @Test
    @DisplayName("개인 기록 통계 조회 - 실제 데이터")
    void getPersonalBestStats_WithData() {
        Long userId = 1L;
        
        List<RunningRecord> records = List.of(
            createTestRunningRecord(1L, 5000.0, 3600L, 7.2), // 가장 긴 거리, 가장 빠른 페이스
            createTestRunningRecord(2L, 3000.0, 2700L, 9.0),
            createTestRunningRecord(3L, 2000.0, 7200L, 6.0)  // 가장 긴 시간
        );
        
        when(runningRecordRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(records);

        PersonalBestStatsResponse result = runningStatsService.getPersonalBestStats(userId);

        assertThat(result).isNotNull();
        assertThat(result.longestDistance()).isEqualTo(5.0); // 5000m -> 5.0km
        assertThat(result.fastestPace()).isEqualTo(6.0);
        assertThat(result.longestTime()).isEqualTo(7200L);
        assertThat(result.totalDistance()).isEqualTo(10.0); // 10000m -> 10.0km
        assertThat(result.totalRuns()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("추천 코스 상세 통계 조회 - 실제 데이터")
    void getRecommendedCourseDetailStats_WithData() {
        Long courseId = 1L;
        Long userId = 1L;
        
        RecommendedCourse course = createTestRecommendedCourse();
        List<RunningRecord> courseRecords = List.of(
            createTestRunningRecordWithUser(1L, 1500.0, 1800L, 8.0, userId),
            createTestRunningRecordWithUser(2L, 1600.0, 1920L, 8.3, 2L),
            createTestRunningRecordWithUser(3L, 1450.0, 1740L, 7.8, userId)
        );
        
        when(recommendedCourseRepository.findByCourseId(courseId)).thenReturn(Optional.of(course));
        when(runningRecordRepository.findByRecommendedCourseIdWithDetails(courseId)).thenReturn(courseRecords);

        RecommendedCourseDetailStats result = runningStatsService.getRecommendedCourseDetailStats(courseId, userId);

        assertThat(result).isNotNull();
        assertThat(result.totalCompletionCount()).isEqualTo(3);
        assertThat(result.uniqueRunnerCount()).isEqualTo(2);
        assertThat(result.myCompletionCount()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("인기 추천 코스 목록 조회 - 실제 데이터")
    void getPopularRecommendedCourses_WithData() {
        List<RunningRecord> allRecords = List.of(
            createTestRunningRecordWithRecommendedCourse(1L, 1L), // 코스 1에 2번 완주
            createTestRunningRecordWithRecommendedCourse(2L, 1L),
            createTestRunningRecordWithRecommendedCourse(3L, 2L)  // 코스 2에 1번 완주
        );
        
        List<RecommendedCourse> allCourses = List.of(
            createTestRecommendedCourseWithId(1L),
            createTestRecommendedCourseWithId(2L)
        );
        
        when(runningRecordRepository.findAllRecommendedCourseRecords()).thenReturn(allRecords);
        when(recommendedCourseRepository.findAllWithUser()).thenReturn(allCourses);

        List<PopularRecommendedCourseResponse> result = runningStatsService.getPopularRecommendedCourses();

        assertThat(result).hasSize(2);
        // 첫 번째 결과가 더 많은 완주 횟수를 가져야 함
        assertThat(result.get(0).totalCompletionCount()).isGreaterThanOrEqualTo(result.get(1).totalCompletionCount());
    }
    
    private RunningRecord createTestRunningRecord(Long id, Double distance, Long time, Double pace) {
        User user = createTestUser();
        RunningRecord record = RunningRecord.builder()
                .user(user)
                .totalDistance(distance)
                .totalTime(time)
                .pace(pace)
                .startedTime(LocalDateTime.now().minusHours(2))
                .endedTime(LocalDateTime.now())
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .startLatitude(37.566)
                .startLongitude(126.977)
                .endLatitude(37.563)
                .endLongitude(126.982)
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .build();

        try {
            java.lang.reflect.Field idField = RunningRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(record, id);
        } catch (Exception e) {
            //무시
        }
        return record;
    }

    private RunningRecord createTestRunningRecord() {
        return createTestRunningRecord(1L, 1500.0, 1800L, 8.0);
    }
    
    private RunningRecord createTestRunningRecordWithUser(Long recordId, Double distance, Long time, Double pace, Long userId) {
        User user = createTestUserWithId(userId);
        RunningRecord record = RunningRecord.builder()
                .user(user)
                .totalDistance(distance)
                .totalTime(time)
                .pace(pace)
                .startedTime(LocalDateTime.now().minusHours(2))
                .endedTime(LocalDateTime.now())
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .startLatitude(37.566)
                .startLongitude(126.977)
                .endLatitude(37.563)
                .endLongitude(126.982)
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .build();

        try {
            java.lang.reflect.Field idField = RunningRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(record, recordId);
        } catch (Exception e) {
            //무시
        }
        return record;
    }
    
    private RunningRecord createTestRunningRecordWithRecommendedCourse(Long id, Long recommendedCourseId) {
        User user = createTestUser();
        RecommendedCourse recommendedCourse = createTestRecommendedCourseWithId(recommendedCourseId);
        
        RunningRecord record = RunningRecord.builder()
                .user(user)
                .recommendedCourse(recommendedCourse)
                .totalDistance(1500.0)
                .totalTime(1800L)
                .pace(8.0)
                .startedTime(LocalDateTime.now().minusHours(2))
                .endedTime(LocalDateTime.now())
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .startLatitude(37.566)
                .startLongitude(126.977)
                .endLatitude(37.563)
                .endLongitude(126.982)
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .build();

        try {
            java.lang.reflect.Field idField = RunningRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(record, id);
        } catch (Exception e) {
            //무시
        }
        return record;
    }
    
    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .name("테스트 유저")
                .nickname("테스트 유저")
                .build();
    }
    
    private User createTestUserWithId(Long id) {
        User user = User.builder()
                .email("test" + id + "@example.com")
                .name("테스트유저" + id)
                .nickname("테스트유저" + id)
                .build();
        
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            //무시
        }
        return user;
    }

    private RecommendedCourse createTestRecommendedCourse() {
        User user = createTestUser();
        return RecommendedCourse.builder()
                .user(user)
                .title("테스트 추천 코스")
                .sourceRecordId(1L)
                .imageUrl("https://example.com/image.jpg")
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .description("테스트용 코스 설명")
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .totalDistance(1500.0)
                .build();
    }
    
    private RecommendedCourse createTestRecommendedCourseWithId(Long id) {
        User user = createTestUser();
        RecommendedCourse course = RecommendedCourse.builder()
                .user(user)
                .title("테스트 추천 코스 #" + id)
                .sourceRecordId(id)
                .imageUrl("https://example.com/image.jpg")
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .description("테스트용 코스 설명")
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .totalDistance(1500.0)
                .build();
        
        try {
            java.lang.reflect.Field idField = RecommendedCourse.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(course, id);
        } catch (Exception e) {
            //무시
        }
        return course;
    }
}
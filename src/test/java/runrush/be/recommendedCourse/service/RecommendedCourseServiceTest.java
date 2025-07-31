package runrush.be.recommendedCourse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.coursebookmark.repository.CourseBookmarkRepository;
import runrush.be.courselike.repository.CourseLikeRepository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.dto.RecommendedCourseListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseMyListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseUpdateRequest;
import runrush.be.recommendedCourse.enums.SortType;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.service.RunningRecordService;
import runrush.be.stats.dto.RecommendedCourseDetailStats;
import runrush.be.stats.dto.CourseTopRunner;
import runrush.be.stats.service.RunningStatsService;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendedCourseServiceTest {

    @Mock
    private RecommendedCourseRepository recommendedCourseRepository;
    
    @Mock
    private RunningRecordService runningRecordService;
    
    @Mock
    private CourseBookmarkRepository courseBookmarkRepository;
    
    @Mock
    private CourseLikeRepository courseLikeRepository;
    
    @Mock
    private RunningStatsService runningStatsService;
    
    @InjectMocks
    private RecommendedCourseService recommendedCourseService;

    @Test
    @DisplayName("추천 코스 생성 성공")
    void createRecommendedCourse_Success() {
        Long recordId = 1L;
        Long userId = 100L;
        String userName = "테스트유저";
        
        RunningRecord runningRecord = createTestRunningRecord();
        when(runningRecordService.validateRunningRecord(recordId, userId)).thenReturn(runningRecord);
        when(recommendedCourseRepository.existsBySourceRecordId(recordId)).thenReturn(false);
        
        assertThatCode(() -> recommendedCourseService.createRecommendedCourse(recordId, userId, userName))
                .doesNotThrowAnyException();
        
        verify(runningRecordService).validateRunningRecord(recordId, userId);
        verify(recommendedCourseRepository).existsBySourceRecordId(recordId);
        verify(recommendedCourseRepository).save(any(RecommendedCourse.class));
    }
    
    @Test
    @DisplayName("추천 코스 생성 실패 - 이미 존재하는 코스")
    void createRecommendedCourse_AlreadyExists() {
        Long recordId = 1L;
        Long userId = 100L;
        String userName = "테스트유저";
        
        RunningRecord runningRecord = createTestRunningRecord();
        when(runningRecordService.validateRunningRecord(recordId, userId)).thenReturn(runningRecord);
        when(recommendedCourseRepository.existsBySourceRecordId(recordId)).thenReturn(true);
        
        assertThatThrownBy(() -> recommendedCourseService.createRecommendedCourse(recordId, userId, userName))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_ALREADY_EXISTS);
                
        verify(recommendedCourseRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("추천 코스 업데이트 성공 - 제목과 설명 모두 변경")
    void updateRecommendedCourse_Success() {
        Long courseId = 1L;
        Long userId = 100L;
        RecommendedCourseUpdateRequest request = new RecommendedCourseUpdateRequest("새로운 제목", "새로운 설명");
        
        RecommendedCourse recommendedCourse = spy(createTestRecommendedCourse());
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        doNothing().when(recommendedCourse).validateOwnership(userId);
        doNothing().when(recommendedCourse).changeTitle(anyString());
        doNothing().when(recommendedCourse).changeDescription(anyString());
        
        assertThatCode(() -> recommendedCourseService.updateRecommendedCourse(courseId, userId, request))
                .doesNotThrowAnyException();
        
        verify(recommendedCourse).validateOwnership(userId);
        verify(recommendedCourse).changeTitle("새로운 제목");
        verify(recommendedCourse).changeDescription("새로운 설명");
    }
    
    @Test
    @DisplayName("추천 코스 업데이트 - 제목만 변경")
    void updateRecommendedCourse_TitleOnly() {
        Long courseId = 1L;
        Long userId = 100L;
        RecommendedCourseUpdateRequest request = new RecommendedCourseUpdateRequest("새로운 제목", null);
        
        RecommendedCourse recommendedCourse = spy(createTestRecommendedCourse());
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        doNothing().when(recommendedCourse).validateOwnership(userId);
        doNothing().when(recommendedCourse).changeTitle(anyString());
        
        assertThatCode(() -> recommendedCourseService.updateRecommendedCourse(courseId, userId, request))
                .doesNotThrowAnyException();
        
        verify(recommendedCourse).changeTitle("새로운 제목");
        verify(recommendedCourse, never()).changeDescription(anyString());
    }
    
    @Test
    @DisplayName("추천 코스 업데이트 실패 - 코스 없음")
    void updateRecommendedCourse_CourseNotFound() {
        Long courseId = 999L;
        Long userId = 100L;
        RecommendedCourseUpdateRequest request = new RecommendedCourseUpdateRequest("제목", "설명");
        
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> recommendedCourseService.updateRecommendedCourse(courseId, userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);
    }
    
    @Test
    @DisplayName("추천 코스 삭제 성공")
    void deleteRecommendedCourse_Success() {
        Long courseId = 1L;
        Long userId = 100L;
        
        RecommendedCourse recommendedCourse = spy(createTestRecommendedCourse());
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        doNothing().when(recommendedCourse).validateOwnership(userId);
        doNothing().when(recommendedCourse).courseDelete();
        
        assertThatCode(() -> recommendedCourseService.deleteRecommendedCourse(courseId, userId))
                .doesNotThrowAnyException();
        
        verify(recommendedCourse).validateOwnership(userId);
        verify(recommendedCourse).courseDelete();
    }
    
    @Test
    @DisplayName("추천 코스 상세 조회 성공")
    void getRecommendedCourseDetail_Success() {
        Long courseId = 1L;
        Long userId = 100L;
        
        RecommendedCourse recommendedCourse = createTestRecommendedCourse();
        when(recommendedCourseRepository.findByCourseId(courseId)).thenReturn(Optional.of(recommendedCourse));
        when(courseLikeRepository.countByRecommendedCourseId(courseId)).thenReturn(5L);
        when(courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(true);
        when(courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(false);
        
        RecommendedCourseResponse result = recommendedCourseService.getRecommendedCourseDetail(courseId, userId);
        
        assertThat(result).isNotNull();
        verify(recommendedCourseRepository).findByCourseId(courseId);
        verify(courseLikeRepository).countByRecommendedCourseId(courseId);
        verify(courseLikeRepository).existsByUserIdAndRecommendedCourseId(userId, courseId);
        verify(courseBookmarkRepository).existsByUserIdAndRecommendedCourseId(userId, courseId);
    }
    
    @Test
    @DisplayName("추천 코스 통계 포함 상세 조회 성공")
    void getRecommendedCourseDetailWithStats_Success() {
        Long courseId = 1L;
        Long userId = 100L;
        
        RecommendedCourse recommendedCourse = createTestRecommendedCourse();
        RecommendedCourseDetailStats stats = createTestStats();
        
        when(recommendedCourseRepository.findByCourseId(courseId)).thenReturn(Optional.of(recommendedCourse));
        when(courseLikeRepository.countByRecommendedCourseId(courseId)).thenReturn(3L);
        when(courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(false);
        when(courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(true);
        when(runningStatsService.getRecommendedCourseDetailStats(courseId, userId)).thenReturn(stats);
        
        RecommendedCourseResponse result = recommendedCourseService.getRecommendedCourseDetailWithStats(courseId, userId);
        
        assertThat(result).isNotNull();
        verify(runningStatsService).getRecommendedCourseDetailStats(courseId, userId);
    }
    
    @Test
    @DisplayName("내 추천 코스 목록 조회 성공")
    void getMyRecommendedCourses_Success() {
        Long userId = 100L;
        List<RecommendedCourse> courses = List.of(createTestRecommendedCourse(), createTestRecommendedCourse());
        
        when(recommendedCourseRepository.findWithUserByUserId(userId)).thenReturn(courses);
        
        List<RecommendedCourseMyListResponse> result = recommendedCourseService.getMyRecommendedCourses(userId);
        
        assertThat(result).hasSize(2);
        verify(recommendedCourseRepository).findWithUserByUserId(userId);
    }
    
    @Test
    @DisplayName("추천 코스 목록 조회 성공 - 좋아요 순")
    void getRecommendedCourses_SortByLike() {
        Long userId = 100L;
        List<RecommendedCourse> courses = List.of(createTestRecommendedCourse());
        List<Long> bookmarkedIds = List.of(1L);
        
        when(recommendedCourseRepository.findAllOrderedByLikeCount(userId)).thenReturn(courses);
        when(courseBookmarkRepository.findRecommendedCourseIdByUserId(userId)).thenReturn(bookmarkedIds);
        
        List<RecommendedCourseListResponse> result = recommendedCourseService.getRecommendedCourses(SortType.LIKE, userId);
        
        assertThat(result).hasSize(1);
        verify(recommendedCourseRepository).findAllOrderedByLikeCount(userId);
        verify(courseBookmarkRepository).findRecommendedCourseIdByUserId(userId);
    }
    
    @Test
    @DisplayName("추천 코스 목록 조회 성공 - 거리 순")
    void getRecommendedCourses_SortByDistance() {
        Long userId = 100L;
        List<RecommendedCourse> courses = List.of(createTestRecommendedCourse());
        List<Long> bookmarkedIds = List.of();
        
        when(recommendedCourseRepository.findAllOrderedByTotalDistance(userId)).thenReturn(courses);
        when(courseBookmarkRepository.findRecommendedCourseIdByUserId(userId)).thenReturn(bookmarkedIds);
        
        List<RecommendedCourseListResponse> result = recommendedCourseService.getRecommendedCourses(SortType.DISTANCE, userId);
        
        assertThat(result).hasSize(1);
        verify(recommendedCourseRepository).findAllOrderedByTotalDistance(userId);
    }
    
    @Test
    @DisplayName("추천 코스 목록 조회 성공 - 최신 순")
    void getRecommendedCourses_SortByRecent() {
        Long userId = 100L;
        List<RecommendedCourse> courses = List.of(createTestRecommendedCourse());
        List<Long> bookmarkedIds = List.of();
        
        when(recommendedCourseRepository.findAllOrderedByCreatedAt(userId)).thenReturn(courses);
        when(courseBookmarkRepository.findRecommendedCourseIdByUserId(userId)).thenReturn(bookmarkedIds);
        
        List<RecommendedCourseListResponse> result = recommendedCourseService.getRecommendedCourses(SortType.RECENT, userId);
        
        assertThat(result).hasSize(1);
        verify(recommendedCourseRepository).findAllOrderedByCreatedAt(userId);
    }
    
    @Test
    @DisplayName("추천 코스 상세 조회 실패 - 코스 없음")
    void getRecommendedCourseDetail_CourseNotFound() {
        Long courseId = 999L;
        Long userId = 100L;
        
        when(recommendedCourseRepository.findByCourseId(courseId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> recommendedCourseService.getRecommendedCourseDetail(courseId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);
    }
    
    @Test
    @DisplayName("추천 코스 업데이트 - 빈 제목 처리")
    void updateRecommendedCourse_EmptyTitle() {
        Long courseId = 1L;
        Long userId = 100L;
        RecommendedCourseUpdateRequest request = new RecommendedCourseUpdateRequest("", "새로운 설명");
        
        RecommendedCourse recommendedCourse = spy(createTestRecommendedCourse());
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        doNothing().when(recommendedCourse).validateOwnership(userId);
        doNothing().when(recommendedCourse).changeDescription(anyString());
        
        assertThatCode(() -> recommendedCourseService.updateRecommendedCourse(courseId, userId, request))
                .doesNotThrowAnyException();
        
        verify(recommendedCourse, never()).changeTitle(anyString());
        verify(recommendedCourse).changeDescription("새로운 설명");
    }
    
    @Test
    @DisplayName("추천 코스 통계 포함 상세 조회 실패 - 코스 없음")
    void getRecommendedCourseDetailWithStats_CourseNotFound() {
        Long courseId = 999L;
        Long userId = 100L;
        
        when(recommendedCourseRepository.findByCourseId(courseId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> recommendedCourseService.getRecommendedCourseDetailWithStats(courseId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);
    }
    
    @Test
    @DisplayName("추천 코스 업데이트 - 공백 제목 처리")
    void updateRecommendedCourse_BlankTitle() {
        Long courseId = 1L;
        Long userId = 100L;
        RecommendedCourseUpdateRequest request = new RecommendedCourseUpdateRequest("   ", "새로운 설명");
        
        RecommendedCourse recommendedCourse = spy(createTestRecommendedCourse());
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        doNothing().when(recommendedCourse).validateOwnership(userId);
        doNothing().when(recommendedCourse).changeDescription(anyString());
        
        assertThatCode(() -> recommendedCourseService.updateRecommendedCourse(courseId, userId, request))
                .doesNotThrowAnyException();
        
        verify(recommendedCourse, never()).changeTitle(anyString());
        verify(recommendedCourse).changeDescription("새로운 설명");
    }
    
    private RunningRecord createTestRunningRecord() {
        User user = createTestUser();
        return RunningRecord.builder()
                .user(user)
                .pathGeoJson("{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}")
                .startLatitude(37.566)
                .startLongitude(126.977)
                .endLatitude(37.563)
                .endLongitude(126.982)
                .startedTime(LocalDateTime.of(2024, 1, 1, 9, 0, 0))
                .endedTime(LocalDateTime.of(2024, 1, 1, 9, 30, 0))
                .totalDistance(1500.0)
                .totalTime(1800L)
                .pace(8.0)
                .startLocationName("시작지점")
                .endLocationName("도착지점")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }
    
    private RecommendedCourse createTestRecommendedCourse() {
        User user = createTestUser();
        RecommendedCourse course = RecommendedCourse.builder()
                .user(user)
                .title("테스트 추천 코스 #1")
                .sourceRecordId(1L)
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
            idField.set(course, 1L);
        } catch (Exception e) {
            //무시
        }
        
        return course;
    }
    
    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }
    
    private RecommendedCourseDetailStats createTestStats() {
        return new RecommendedCourseDetailStats(
                1L, // courseId
                "테스트 추천 코스 #1", // courseTitle
                "테스트유저", // creatorName
                1.5, // courseDistanceKm
                10, // totalCompletionCount
                5,  // uniqueRunnerCount
                1800.0, // averageCompletionTimeSeconds
                8.0, // averagePace
                2,  // myCompletionCount
                1700.0, // myBestTimeSeconds
                7.5, // myAveragePace
                List.of(new CourseTopRunner("러너1", 1600L, 7.0), new CourseTopRunner("러너2", 1650L, 7.2)) // topRunners
        );
    }
}
package runrush.be.runningrecord.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningCalculator;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.domain.RunningRecordFactory;
import runrush.be.runningrecord.dto.RunningRecordListResponse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.runningrecord.dto.RunningRecordResponse;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.s3.service.ImageUploadService;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunningRecordServiceTest {

    @Mock
    private RunningRecordRepository runningRecordRepository;
    
    @Mock
    private RecommendedCourseRepository recommendedCourseRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ImageUploadService imageUploadService;
    
    @Mock
    private RunningCalculator runningCalculator;
    
    @Mock
    private RunningRecordFactory runningRecordFactory;
    
    @Mock
    private MultipartFile image;
    
    @InjectMocks
    private RunningRecordService runningRecordService;

    @Test
    @DisplayName("러닝 기록 저장 성공 - 기본 케이스")
    void saveRunningRecord_Success() {
        Long userId = 100L;
        RunningRecordRequest request = createTestRequest();
        String imageUrl = "https://example.com/image.jpg";
        
        User user = createTestUser();
        RunningRecord runningRecord = createTestRunningRecord();
        
        when(userService.findUserById(userId)).thenReturn(user);
        when(imageUploadService.uploadImage(image, "running-record")).thenReturn(imageUrl);
        when(runningRecordFactory.createRunningRecord(request, user, null, imageUrl)).thenReturn(runningRecord);
        when(runningCalculator.calculateExperiencePoints(1500.0)).thenReturn(150);
        
        assertThatCode(() -> runningRecordService.saveRunningRecord(request, userId, image))
                .doesNotThrowAnyException();
        
        verify(userService).findUserById(userId);
        verify(runningRecordRepository).save(runningRecord);
    }
    
    @Test
    @DisplayName("러닝 기록 저장 실패 - 사용자 없음")
    void saveRunningRecord_UserNotFound() {
        Long userId = 999L;
        RunningRecordRequest request = createTestRequest();
        
        when(userService.findUserById(userId)).thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        assertThatThrownBy(() -> runningRecordService.saveRunningRecord(request, userId, image))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
    
    @Test
    @DisplayName("이미지 업로드 서비스 호출 확인")
    void saveRunningRecord_ImageUploadCalled() {
        Long userId = 100L;
        RunningRecordRequest request = createTestRequest();
        String imageUrl = "uploaded-image.jpg";
        
        User user = createTestUser();
        RunningRecord runningRecord = createTestRunningRecord();
        
        when(userService.findUserById(userId)).thenReturn(user);
        when(imageUploadService.uploadImage(image, "running-record")).thenReturn(imageUrl);
        when(runningRecordFactory.createRunningRecord(eq(request), eq(user), isNull(), eq(imageUrl))).thenReturn(runningRecord);
        when(runningCalculator.calculateExperiencePoints(anyDouble())).thenReturn(100);
        
        runningRecordService.saveRunningRecord(request, userId, image);
        
        verify(imageUploadService).uploadImage(image, "running-record");
        verify(runningRecordFactory).createRunningRecord(request, user, null, imageUrl);
    }
    
    @Test
    @DisplayName("경험치 계산 로직 호출 확인")
    void saveRunningRecord_ExperienceCalculation() {
        Long userId = 100L;
        RunningRecordRequest request = createTestRequest();
        
        User user = spy(createTestUser());
        RunningRecord runningRecord = createTestRunningRecord();
        
        when(userService.findUserById(userId)).thenReturn(user);
        when(imageUploadService.uploadImage(any(), anyString())).thenReturn("image.jpg");
        when(runningRecordFactory.createRunningRecord(any(), any(), any(), anyString())).thenReturn(runningRecord);
        when(runningCalculator.calculateExperiencePoints(1500.0)).thenReturn(150);
        
        runningRecordService.saveRunningRecord(request, userId, image);
        
        verify(runningCalculator).calculateExperiencePoints(1500.0);
        verify(user).addExperiencePoints(150);
    }

    @Test
    @DisplayName("러닝 기록 조회 성공")
    void getRunningRecord_Success() {
        Long recordId = 1L;
        String requestEmail = "test@example.com";
        RunningRecord mockRecord = mock(RunningRecord.class);
        User mockUser = mock(User.class);

        when(mockUser.getEmail()).thenReturn(requestEmail);
        when(mockRecord.getUser()).thenReturn(mockUser);

        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.of(mockRecord));
        when(recommendedCourseRepository.existsBySourceRecordIdAndIsDeletedFalse(recordId))
                .thenReturn(true);

        RunningRecordResponse result = runningRecordService.getRunningRecord(recordId, requestEmail);

        assertThat(result).isNotNull();

        verify(runningRecordRepository).findByIdAndIsDeletedFalse(recordId);
        verify(recommendedCourseRepository).existsBySourceRecordIdAndIsDeletedFalse(recordId);
    }

    @Test
    @DisplayName("러닝 기록 조회 실패 - 기록 없음")
    void getRunningRecord_NotFound() {
        Long recordId = 999L;
        String requestEmail = "test@example.com";

        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningRecordService.getRunningRecord(recordId, requestEmail))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_NOT_FOUND);

        verify(runningRecordRepository).findByIdAndIsDeletedFalse(recordId);
        verify(recommendedCourseRepository, never()).existsBySourceRecordIdAndIsDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("러닝 기록 조회 실패 - 권한 없음")
    void getRunningRecord_AccessDenied() {
        Long recordId = 1L;
        String requestEmail = "test@example.com";
        String ownerEmail = "owner@example.com";

        User recordOwner = mock(User.class);
        when(recordOwner.getEmail()).thenReturn(ownerEmail);

        RunningRecord runningRecord = mock(RunningRecord.class);
        when(runningRecord.getUser()).thenReturn(recordOwner);

        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.of(runningRecord));

        assertThatThrownBy(() -> runningRecordService.getRunningRecord(recordId, requestEmail))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_ACCESS_DENIED);
    }

    @Test
    @DisplayName("러닝 기록 조회 - 추천 코스 미등록")
    void getRunningRecord_NotRegisteredAsCourse() {
        Long recordId = 1L;
        String requestEmail = "test@example.com";

        User user = mock(User.class);
        when(user.getEmail()).thenReturn(requestEmail);

        RunningRecord runningRecord = mock(RunningRecord.class);
        when(runningRecord.getUser()).thenReturn(user);

        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.of(runningRecord));

        when(recommendedCourseRepository.existsBySourceRecordIdAndIsDeletedFalse(recordId))
                .thenReturn(false);

        RunningRecordResponse result = runningRecordService.getRunningRecord(recordId, requestEmail);

        assertThat(result).isNotNull();
        assertThat(result.isRegisteredAsCourse()).isFalse();

        verify(runningRecordRepository).findByIdAndIsDeletedFalse(recordId);
        verify(recommendedCourseRepository).existsBySourceRecordIdAndIsDeletedFalse(recordId);
    }

    @Test
    @DisplayName("러닝 기록 목록 조회 성공")
    void getRunningRecords_Success() {
        Long userId = 1L;

        RunningRecord record1 = mock(RunningRecord.class);
        RunningRecord record2 = mock(RunningRecord.class);

        List<RunningRecord> records = List.of(record1, record2);

        when(runningRecordRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(records);

        RunningRecordListResponse response1 = mock(RunningRecordListResponse.class);
        RunningRecordListResponse response2 = mock(RunningRecordListResponse.class);

        List<RunningRecordListResponse> result = runningRecordService.getRunningRecords(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(runningRecordRepository).findByUserIdAndIsDeletedFalse(userId);
    }

    @Test
    @DisplayName("러닝 기록 목록 조회 - 빈 목록")
    void getRunningRecords_EmptyList() {
        Long userId = 1L;

        when(runningRecordRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of());

        List<RunningRecordListResponse> result = runningRecordService.getRunningRecords(userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(runningRecordRepository).findByUserIdAndIsDeletedFalse(userId);
    }

    @Test
    @DisplayName("러닝 기록 삭제 성공")
    void deleteRunningRecord_Success() {
        Long recordId = 1L;
        Long userId = 1L;

        RunningRecord mockRecord = mock(RunningRecord.class);
        User mockUser = mock(User.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockRecord.getUser()).thenReturn(mockUser);
        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.of(mockRecord));

        assertThatCode(() -> runningRecordService.deleteRunningRecord(recordId, userId))
                .doesNotThrowAnyException();

        verify(mockRecord).recordDelete();
        verify(runningRecordRepository).findByIdAndIsDeletedFalse(recordId);
    }

    @Test
    @DisplayName("러닝 기록 삭제 실패 - 기록 없음")
    void deleteRunningRecord_NotFound() {
        Long recordId = 999L;
        Long userId = 1L;

        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningRecordService.deleteRunningRecord(recordId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("러닝 기록 삭제 실패 - 권한 없음")
    void deleteRunningRecord_AccessDenied() {
        Long recordId = 1L;
        Long requestUserId = 1L;
        Long ownerUserId = 2L;

        RunningRecord mockRecord = mock(RunningRecord.class);
        User recordOwner = mock(User.class);

        when(recordOwner.getId()).thenReturn(ownerUserId);
        when(mockRecord.getUser()).thenReturn(recordOwner);
        when(runningRecordRepository.findByIdAndIsDeletedFalse(recordId))
                .thenReturn(Optional.of(mockRecord));

        assertThatThrownBy(() -> runningRecordService.deleteRunningRecord(recordId, requestUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_ACCESS_DENIED);
    }

    @Test
    @DisplayName("삭제된 러닝 기록 조회 성공")
    void getDeletedRecord_Success() {
        Long userId = 1L;

        RunningRecord deletedRecord1 = mock(RunningRecord.class);
        RunningRecord deletedRecord2 = mock(RunningRecord.class);

        List<RunningRecord> deletedRecords = List.of(deletedRecord1, deletedRecord2);

        when(runningRecordRepository.findByUserIdAndIsDeletedTrue(userId))
                .thenReturn(deletedRecords);

        List<RunningRecordListResponse> result = runningRecordService.getDeletedRecord(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(runningRecordRepository).findByUserIdAndIsDeletedTrue(userId);
    }

    @Test
    @DisplayName("삭제된 러닝 기록 조회 - 빈 목록")
    void getDeletedRecord_EmptyList() {
        Long userId = 1L;

        when(runningRecordRepository.findByUserIdAndIsDeletedTrue(userId))
                .thenReturn(List.of());

        List<RunningRecordListResponse> result = runningRecordService.getDeletedRecord(userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(runningRecordRepository).findByUserIdAndIsDeletedTrue(userId);
    }

    @Test
    @DisplayName("러닝 기록 복구 성공")
    void restoreRunningRecord_Success() {
        Long recordId = 1L;
        Long userId = 1L;

        RunningRecord mockRecord = mock(RunningRecord.class);

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.of(mockRecord));

        assertThatCode(() -> runningRecordService.restoreRunningRecord(recordId, userId))
                .doesNotThrowAnyException();

        verify(mockRecord).restore();
        verify(runningRecordRepository).findByIdAndUserIdAndIsDeletedTrue(recordId, userId);
    }

    @Test
    @DisplayName("러닝 기록 복구 실패 - 기록 없음")
    void restoreRunningRecord_NotFound() {
        Long recordId = 999L;
        Long userId = 1L;

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningRecordService.restoreRunningRecord(recordId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_RESTORE_FAILED);
    }

    @Test
    @DisplayName("러닝 기록 영구 삭제 성공 - 이미지 없음")
    void permanentlyDeleteRecord_Success_NoImage() {
        Long recordId = 1L;
        Long userId = 1L;

        RunningRecord mockRecord = mock(RunningRecord.class);
        when(mockRecord.getImageUrl()).thenReturn(null);

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.of(mockRecord));

        assertThatCode(() -> runningRecordService.permanentlyDeleteRecord(recordId, userId))
                .doesNotThrowAnyException();

        verify(runningRecordRepository).delete(mockRecord);
        verify(imageUploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("러닝 기록 영구 삭제 성공 - 이미지 포함")
    void permanentlyDeleteRecord_Success_WithImage() {
        Long recordId = 1L;
        Long userId = 1L;
        String imageUrl = "https://example.com/image.jpg";

        RunningRecord mockRecord = mock(RunningRecord.class);
        when(mockRecord.getImageUrl()).thenReturn(imageUrl);

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.of(mockRecord));

        assertThatCode(() -> runningRecordService.permanentlyDeleteRecord(recordId, userId))
                .doesNotThrowAnyException();

        verify(runningRecordRepository).delete(mockRecord);
        verify(imageUploadService).deleteImage(imageUrl);
    }

    @Test
    @DisplayName("러닝 기록 영구 삭제 - 이미지 삭제 실패해도 기록은 삭제")
    void permanentlyDeleteRecord_ImageDeleteFails() {
        Long recordId = 1L;
        Long userId = 1L;
        String imageUrl = "https://example.com/image.jpg";

        RunningRecord mockRecord = mock(RunningRecord.class);
        when(mockRecord.getImageUrl()).thenReturn(imageUrl);

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.of(mockRecord));
        doThrow(new RuntimeException("S3 삭제 실패")).when(imageUploadService).deleteImage(imageUrl);

        assertThatCode(() -> runningRecordService.permanentlyDeleteRecord(recordId, userId))
                .doesNotThrowAnyException();

        verify(runningRecordRepository).delete(mockRecord);
        verify(imageUploadService).deleteImage(imageUrl);
    }

    @Test
    @DisplayName("러닝 기록 영구 삭제 실패 - 기록 없음")
    void permanentlyDeleteRecord_NotFound() {
        Long recordId = 999L;
        Long userId = 1L;

        when(runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningRecordService.permanentlyDeleteRecord(recordId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RUNNING_RECORD_DELETE_FAILED);
    }

    @Test
    @DisplayName("추천 코스 조회 성공 - 존재하는 코스")
    void saveRunningRecord_WithExistingRecommendedCourse() {
        Long userId = 100L;
        Long courseId = 1L;
        
        RunningRecordRequest request = new RunningRecordRequest(
                "{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}",
                37.566, 126.977, 37.563, 126.982,
                LocalDateTime.of(2024, 1, 1, 9, 0, 0),
                LocalDateTime.of(2024, 1, 1, 9, 30, 0),
                courseId // 추천 코스 ID 포함
        );
        
        String imageUrl = "https://example.com/image.jpg";
        
        User user = createTestUser();
        RunningRecord runningRecord = createTestRunningRecord();
        
        RecommendedCourse recommendedCourse = mock(RecommendedCourse.class);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(recommendedCourse));
        
        when(userService.findUserById(userId)).thenReturn(user);
        when(imageUploadService.uploadImage(image, "running-record")).thenReturn(imageUrl);
        when(runningRecordFactory.createRunningRecord(request, user, recommendedCourse, imageUrl)).thenReturn(runningRecord);
        when(runningCalculator.calculateExperiencePoints(1500.0)).thenReturn(150);
        
        assertThatCode(() -> runningRecordService.saveRunningRecord(request, userId, image))
                .doesNotThrowAnyException();
        
        verify(recommendedCourseRepository).findById(courseId);
        verify(runningRecordFactory).createRunningRecord(request, user, recommendedCourse, imageUrl);
    }

    @Test
    @DisplayName("추천 코스 조회 실패 - 존재하지 않는 코스")
    void saveRunningRecord_RecommendedCourseNotFound() {
        Long userId = 100L;
        Long courseId = 999L;
        
        RunningRecordRequest request = new RunningRecordRequest(
                "{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}",
                37.566, 126.977, 37.563, 126.982,
                LocalDateTime.of(2024, 1, 1, 9, 0, 0),
                LocalDateTime.of(2024, 1, 1, 9, 30, 0),
                courseId // 존재하지 않는 코스 ID
        );
        
        User user = createTestUser();
        
        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> runningRecordService.saveRunningRecord(request, userId, image))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);
        
        verify(recommendedCourseRepository).findById(courseId);
        verify(runningRecordFactory, never()).createRunningRecord(any(), any(), any(), any());
    }

    private RunningRecordRequest createTestRequest() {
        return new RunningRecordRequest(
                "{\"type\": \"LineString\", \"coordinates\": [[126.977, 37.566], [126.982, 37.563]]}",
                37.566, 126.977, 37.563, 126.982,
                LocalDateTime.of(2024, 1, 1, 9, 0, 0),
                LocalDateTime.of(2024, 1, 1, 9, 30, 0),
                null
        );
    }
    
    private RunningRecord createTestRunningRecord() {
        return RunningRecord.builder()
                .user(createTestUser())
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
    
    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }
}
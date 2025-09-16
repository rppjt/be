package runrush.be.runningrecord.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningRecordService {
    private final RunningRecordRepository runningRecordRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final UserService userService;
    private final ImageUploadService imageUploadService;
    private final RunningRecordFactory runningRecordFactory;
    private final RunningCalculator runningCalculator;

    @Transactional
    public void saveRunningRecord(RunningRecordRequest request, Long userId, MultipartFile image) {
        // 멱등성 키 검증
        if (request.idempotencyKey() == null || request.idempotencyKey().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        try {
            // 멱등성 검사: 이미 같은 요청이 처리되었는지 확인
            if (runningRecordRepository.existsByIdempotencyKey(request.idempotencyKey())) {
                log.info("이미 처리된 러닝 기록 요청: idempotencyKey={}", request.idempotencyKey());
                return; // 멱등성 보장: 중복 요청은 무시
            }
            
            User user = userService.findUserById(userId);
            RecommendedCourse recommendedCourse = findRecommendedCourse(request.recommendedCourseId());
            String imageUrl = imageUploadService.uploadImage(image, "running-record");

            RunningRecord runningRecord = runningRecordFactory.createRunningRecord(
                request, user, recommendedCourse, imageUrl);

            // 경험치 업데이트와 기록 저장을 하나의 트랜잭션에서 처리
            double totalDistance = runningRecord.getTotalDistance();
            int experiencePoints = runningCalculator.calculateExperiencePoints(totalDistance);
            user.addExperiencePoints(experiencePoints);
            
            runningRecordRepository.save(runningRecord);
            log.info("러닝 기록 저장 완료: idempotencyKey={}, userId={}", request.idempotencyKey(), userId);
            
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약조건 위반 = 동시 요청으로 같은 멱등성 키 사용
            log.info("멱등성 키 중복 감지: idempotencyKey={}", request.idempotencyKey());
            handleConcurrentRunningRecordSave(request.idempotencyKey());
            
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 실패 = 사용자 경험치 동시 업데이트
            log.info("사용자 경험치 업데이트 중 동시성 충돌: userId={}", userId);
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION_ERROR);
        }
    }

    private RecommendedCourse findRecommendedCourse(Long recommendedCourseId) {
        if (recommendedCourseId == null) {
            return null;
        }
        return recommendedCourseRepository.findById(recommendedCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public RunningRecordResponse getRunningRecord(Long recordId, String email) {
        RunningRecord runningRecord = runningRecordRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_RECORD_NOT_FOUND));

        if (!runningRecord.getUser().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.RUNNING_RECORD_ACCESS_DENIED);
        }

        boolean isRegisteredAsCourse = recommendedCourseRepository.existsBySourceRecordIdAndIsDeletedFalse(recordId);

        return RunningRecordResponse.toRecordResponse(runningRecord, isRegisteredAsCourse);
    }

    @Transactional(readOnly = true)
    public List<RunningRecordListResponse> getDeletedRecord(Long userId) {
        return runningRecordRepository.findByUserIdAndIsDeletedTrue(userId).stream()
                .map(RunningRecordListResponse::toRecordListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunningRecordListResponse> getRunningRecords(Long userId) {
        return runningRecordRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .map(RunningRecordListResponse::toRecordListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RunningRecord validateRunningRecord(Long recordId, Long userId) {
        RunningRecord runningRecord = runningRecordRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_RECORD_NOT_FOUND));

        if (!runningRecord.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.RUNNING_RECORD_ACCESS_DENIED);
        }

        return runningRecord;
    }

    @Transactional
    public void deleteRunningRecord(Long recordId, Long userId) {
        RunningRecord runningRecord = validateRunningRecord(recordId, userId);
        runningRecord.recordDelete();
        log.info("러닝 기록 삭제 완료: recordId={}", recordId);
    }

    @Transactional
    public void restoreRunningRecord(Long recordId, Long userId) {
        RunningRecord record = runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_RECORD_RESTORE_FAILED));

        record.restore();
        log.info("러닝 기록 복구 완료: recordId={}", recordId);
    }

    @Transactional
    public void permanentlyDeleteRecord(Long recordId, Long userId) {
        RunningRecord record = runningRecordRepository.findByIdAndUserIdAndIsDeletedTrue(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_RECORD_DELETE_FAILED));

        if (record.getImageUrl() != null) {
            try {
                imageUploadService.deleteImage(record.getImageUrl());
            } catch (Exception e) {
                log.warn("이미지 삭제 중 오류 발생: {}", e.getMessage());
            }
        }

        runningRecordRepository.delete(record);
        log.info("러닝 기록 영구 삭제 완료: recordId={}", recordId);
    }
    
    private void handleConcurrentRunningRecordSave(String idempotencyKey) {
        // 동시 저장 요청 발생 시 처리
        Optional<RunningRecord> existingRecord = runningRecordRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existingRecord.isPresent()) {
            // 이미 저장된 기록이 있으므로 멱등성 보장됨
            log.info("멱등성 복구 - 이미 저장된 러닝 기록 존재: idempotencyKey={}", idempotencyKey);
            return; // 조용히 성공으로 처리
        } else {
            // 기록이 없다면 예상치 못한 상황
            log.warn("멱등성 키는 중복이지만 기록이 없음: idempotencyKey={}", idempotencyKey);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
package runrush.be.runningrecord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.common.util.GeoUtils;
import runrush.be.kakao.client.KakaoMapApiClient;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.dto.RunningRecordListResponse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.runningrecord.dto.RunningRecordResponse;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.s3.service.ImageUploadService;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningRecordService {
    private final RunningRecordRepository runningRecordRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final UserService userService;
    private final KakaoMapApiClient kakaoMapApiClient;
    private final ImageUploadService imageUploadService;

    @Transactional
    public void saveRunningRecord(RunningRecordRequest request, Long userId, MultipartFile image) {
        User user = userService.findUserById(userId);

        if (request.startedTime().isAfter(request.endedTime())) {
            throw new BusinessException(ErrorCode.INVALID_RUNNING_TIME);
        }

        if (Duration.between(request.startedTime(), request.endedTime()).getSeconds() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_RUNNING_TIME);
        }

        RecommendedCourse recommendedCourse = null;
        if (request.recommendedCourseId() != null) {
            recommendedCourse = recommendedCourseRepository.findById(request.recommendedCourseId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));
        }

        String imageUrl = imageUploadService.uploadImage(image, "running-record");

        double totalDistance = calculateTotalDistance(request.pathGeoJson());
        long totalTime = Duration.between(request.startedTime(), request.endedTime()).getSeconds();

        int exp = (int) (totalDistance / 10);
        user.addExperiencePoints(exp);

        BigDecimal minutes = BigDecimal.valueOf(totalTime)
                .divide(BigDecimal.valueOf(60), 10, RoundingMode.HALF_UP);
        BigDecimal kilometers = BigDecimal.valueOf(totalDistance)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);

        BigDecimal paceDecimal = minutes.divide(kilometers, 2, RoundingMode.HALF_UP);
        double pace = paceDecimal.doubleValue();

        String startLocationName = kakaoMapApiClient.reverseGeocode(request.startLatitude(), request.endLongitude());
        String endLocationName = kakaoMapApiClient.reverseGeocode(request.endLatitude(), request.endLongitude());

        RunningRecord runningRecord = RunningRecord.builder()
                .user(user)
                .recommendedCourse(recommendedCourse)
                .imageUrl(imageUrl)
                .pathGeoJson(request.pathGeoJson())
                .totalDistance(totalDistance)
                .startLatitude(request.startLatitude())
                .startLongitude(request.startLongitude())
                .endLatitude(request.endLatitude())
                .endLongitude(request.endLongitude())
                .startLocationName(startLocationName)
                .endLocationName(endLocationName)
                .startedTime(request.startedTime())
                .endedTime(request.endedTime())
                .totalTime(totalTime)
                .pace(pace)
                .build();

        runningRecordRepository.save(runningRecord);
        log.info("러닝 기록 저장 완료");
    }

    @Transactional(readOnly = true)
    public RunningRecordResponse getRunningRecord(Long recordId, String email) {
        RunningRecord runningRecord = runningRecordRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_RECORD_NOT_FOUND));

        if (!runningRecord.getUser().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.RUNNING_RECORD_ACCESS_DENIED);
        }

        return RunningRecordResponse.toRecordResponse(runningRecord);
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

    private double calculateTotalDistance(String pathGeoJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(pathGeoJson);

            String type = jsonNode.get("type").asText();
            if (!"LineString".equals(type)) {
                throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
            }

            JsonNode coordinates = jsonNode.get("coordinates");
            if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
                throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
            }

            double totalDistance = 0.0;

            for (int i = 1; i < coordinates.size(); i++) {
                JsonNode prev = coordinates.get(i - 1);
                JsonNode curr = coordinates.get(i);

                double lon1 = prev.get(0).asDouble();
                double lat1 = prev.get(1).asDouble();
                double lon2 = curr.get(0).asDouble();
                double lat2 = curr.get(1).asDouble();

                totalDistance += GeoUtils.calculateDistanceInMeters(lat1, lon1, lat2, lon2);
            }
            return totalDistance;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GeoJSON 파싱 오류: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_PATH_DATA);
        }
    }
}
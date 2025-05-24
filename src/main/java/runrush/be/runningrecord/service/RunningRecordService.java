package runrush.be.runningrecord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.dto.RunningRecordListResponse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.runningrecord.dto.RunningRecordResponse;
import runrush.be.runningrecord.repository.RunningRecordRepository;
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
    private final UserService userService;

    @Transactional
    public void saveRunningRecord(RunningRecordRequest request, Long userId) {
        User user = userService.findUserById(userId);

        double totalDistance = calculateTotalDistance(request.pathGeoJson());
        long totalTime = Duration.between(request.startedTime(), request.endedTime()).getSeconds();

        BigDecimal minutes = BigDecimal.valueOf(totalTime)
                .divide(BigDecimal.valueOf(60), 10, RoundingMode.HALF_UP);
        BigDecimal kilometers = BigDecimal.valueOf(totalDistance)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);

        BigDecimal paceDecimal = minutes.divide(kilometers, 2, RoundingMode.HALF_UP);
        double pace = paceDecimal.doubleValue();

        RunningRecord runningRecord = RunningRecord.builder()
                .user(user)
                .pathGeoJson(request.pathGeoJson())
                .totalDistance(totalDistance)
                .startLatitude(request.startLatitude())
                .startLongitude(request.startLongitude())
                .endLatitude(request.endLatitude())
                .endLongitude(request.endLongitude())
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 기록입니다."));

        if (!runningRecord.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 기록만 조회할 수 있습니다.");
        }

        return RunningRecordResponse.toRecordResponse(runningRecord);
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
                .orElseThrow(() -> new IllegalArgumentException("기록이 존재하지 않거나 삭제되었습니다."));

        if (!runningRecord.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 기록만 추천할 수 있습니다.");
        }

        return runningRecord;
    }

    @Transactional
    public void deleteRunningRecord(Long recordId, Long userId) {
        RunningRecord runningRecord = validateRunningRecord(recordId, userId);
        runningRecord.recordDeleted();
        log.info("러닝 기록 삭제 완료: recordId={}", recordId);
    }


    private double calculateTotalDistance(String pathGeoJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(pathGeoJson);

            String type = jsonNode.get("type").asText();
            if (!"LineString".equals(type)) {
                throw new IllegalArgumentException("잘못된 경로 형식입니다. LineString 형식이어야 합니다.");
            }

            JsonNode coordinates = jsonNode.get("coordinates");
            if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
                throw new IllegalArgumentException("경로 좌표가 올바르지 않습니다.");
            }

            double totalDistance = 0.0;

            for (int i = 1; i < coordinates.size(); i++) {
                JsonNode prev = coordinates.get(i - 1);
                JsonNode curr = coordinates.get(i);

                double lon1 = prev.get(0).asDouble();
                double lat1 = prev.get(1).asDouble();
                double lon2 = curr.get(0).asDouble();
                double lat2 = curr.get(1).asDouble();

                totalDistance += haversine(lat1, lon1, lat2, lon2);
            }
            return totalDistance;
        } catch (Exception e) {
            log.error("GeoJSON 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("경로 정보를 처리하는 중 오류가 발생했습니다", e);
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }
}
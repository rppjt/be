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

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningRecordService {
    private final RunningRecordRepository runningRecordRepository;
    private final UserService userService;

    @Transactional
    public void saveRunningRecord(RunningRecordRequest request, String email) {
        User user = userService.findUserByEmail(email);

        double totalDistance = calculateTotalDistance(request.pathGeoJson());
        long totalTime = Duration.between(request.startedTime(), request.endedTime()).getSeconds();
        double pace = Math.round(((double) totalTime / 60) / (totalDistance / 1000) * 100) / 100.0;

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
        log.info("러닝 기록 저장 완료 : {}", email);
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
    public List<RunningRecordListResponse> getRunningRecords(String email) {
        return runningRecordRepository.findByUserEmailAndIsDeletedFalse(email).stream()
                .map(RunningRecordListResponse::toRecordListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RunningRecord validateRunningRecord(Long recordId, String email) {
        RunningRecord runningRecord = runningRecordRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록이 존재하지 않거나 삭제되었습니다."));

        if (!runningRecord.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 기록만 추천할 수 있습니다.");
        }

        return runningRecord;
    }

    @Transactional
    public void deleteRunningRecord(Long recordId, String email) {
        RunningRecord runningRecord = validateRunningRecord(recordId, email);
        runningRecord.recordDeleted();
        log.info("러닝 기록 삭제 완료: recordId={}, email={}", recordId, email);
    }


    private double calculateTotalDistance(String pathGeoJson) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(pathGeoJson);
            JsonNode coordinates = jsonNode.get("coordinates");

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
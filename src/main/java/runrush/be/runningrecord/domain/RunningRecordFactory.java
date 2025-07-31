package runrush.be.runningrecord.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.user.domain.User;

@Component
@RequiredArgsConstructor
public class RunningRecordFactory {
    
    private final RunningCalculator runningCalculator;
    private final LocationResolver locationResolver;

    public RunningRecord createRunningRecord(
            RunningRecordRequest request,
            User user,
            RecommendedCourse recommendedCourse,
            String imageUrl) {
        
        /*
         러닝 시간 검증
         */
        runningCalculator.validateRunningTime(request.startedTime(), request.endedTime());
        
        /*
         거리 및 시간 계산
         */
        double totalDistance = runningCalculator.calculateTotalDistance(request.pathGeoJson());
        long totalTime = runningCalculator.calculateTotalTimeInSeconds(request.startedTime(), request.endedTime());
        double pace = runningCalculator.calculatePace(totalDistance, totalTime);
        
        /*
         위치 정보 조회
         */
        String startLocationName = locationResolver.resolveLocationName(
            request.startLatitude(), request.startLongitude());
        String endLocationName = locationResolver.resolveLocationName(
            request.endLatitude(), request.endLongitude());
        
        return RunningRecord.builder()
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
    }
}
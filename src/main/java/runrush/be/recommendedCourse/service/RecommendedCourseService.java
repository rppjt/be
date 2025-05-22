package runrush.be.recommendedCourse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.service.RunningRecordService;

@Service
@RequiredArgsConstructor
public class RecommendedCourseService {
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final RunningRecordService runningRecordService;

    public void createRecommendedCourse(Long recordId, String email, String name) {
        RunningRecord runningRecord = runningRecordService.validateRunningRecord(recordId, email);

        if (recommendedCourseRepository.existsByRunningRecord(runningRecord)) {
            throw new IllegalArgumentException("이미 추천된 기록입니다.");
        }

        String title = name + "님의 추천 코스 #" + recordId;

        RecommendedCourse course = RecommendedCourse.builder()
                .title(title)
                .description("")
                .pathGeoJson(runningRecord.getPathGeoJson())
                .totalDistance(runningRecord.getTotalDistance())
                .pace(runningRecord.getPace())
                .latitude(runningRecord.getStartLatitude())
                .longitude(runningRecord.getStartLongitude())
                .build();

        recommendedCourseRepository.save(course);
    }
}
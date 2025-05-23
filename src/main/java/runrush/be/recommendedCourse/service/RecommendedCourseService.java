package runrush.be.recommendedCourse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.dto.RecommendedCourseListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseUpdateRequest;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.service.RunningRecordService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendedCourseService {
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final RunningRecordService runningRecordService;

    @Transactional
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
                .latitude(runningRecord.getStartLatitude())
                .longitude(runningRecord.getStartLongitude())
                .build();

        recommendedCourseRepository.save(course);
    }

    @Transactional
    public void updateRecommendedCourse(Long courseId, String email, RecommendedCourseUpdateRequest request) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getEmail().equals(email))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        if(request.title() != null && !request.title().isBlank()) {
            recommendedCourse.changeTitle(request.title());
        }

        if(request.description() != null) {
            recommendedCourse.changeDescription(request.description());
        }
    }

    @Transactional
    public void deleteRecommendedCourse(Long courseId, String email) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getEmail().equals(email))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        recommendedCourse.delete();
    }

    @Transactional(readOnly = true)
    public RecommendedCourseResponse getRecommendedCourse(Long courseId, String email) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getEmail().equals(email))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        return RecommendedCourseResponse.toCourseResponse(recommendedCourse);
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseListResponse> getUserRecommendedCourses(String email) {
        return recommendedCourseRepository.findByUserEmailAndIsDeletedFalse(email).stream()
                .map(RecommendedCourseListResponse::toCourseListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseListResponse> getRecommendedCourses() {
        return recommendedCourseRepository.findAllByIsDeletedFalse().stream()
                .map(RecommendedCourseListResponse::toCourseListResponse)
                .toList();
    }
}
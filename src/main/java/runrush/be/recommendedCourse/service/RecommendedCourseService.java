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
    public void createRecommendedCourse(Long recordId, Long userId, String name) {
        RunningRecord runningRecord = runningRecordService.validateRunningRecord(recordId, userId);

        if (recommendedCourseRepository.existsBySourceRecordId(recordId)) {
            throw new IllegalArgumentException("이미 추천된 기록입니다.");
        }

        String title = name + "님의 추천 코스 #" + recordId;

        RecommendedCourse course = RecommendedCourse.builder()
                .title(title)
                .description("")
                .pathGeoJson(runningRecord.getPathGeoJson())
                .totalDistance(runningRecord.getTotalDistance())
                .latitude(runningRecord.getEndLatitude())
                .longitude(runningRecord.getEndLongitude())
                .build();

        recommendedCourseRepository.save(course);
    }

    @Transactional
    public void updateRecommendedCourse(Long courseId, Long userId, RecommendedCourseUpdateRequest request) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getId().equals(userId))) {
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
    public void deleteRecommendedCourse(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        recommendedCourseRepository.delete(recommendedCourse);
    }

    @Transactional(readOnly = true)
    public RecommendedCourseResponse getRecommendedCourse(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if(!(recommendedCourse.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        return RecommendedCourseResponse.toCourseResponse(recommendedCourse);
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseListResponse> getUserRecommendedCourses(Long userId) {
        return recommendedCourseRepository.findByUserId(userId).stream()
                .map(RecommendedCourseListResponse::toCourseListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseListResponse> getRecommendedCourses() {
        return recommendedCourseRepository.findAll().stream()
                .map(RecommendedCourseListResponse::toCourseListResponse)
                .toList();
    }
}
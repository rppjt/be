package runrush.be.recommendedCourse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendedCourseService {
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final RunningRecordService runningRecordService;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseLikeRepository courseLikeRepository;

    @Transactional
    public void createRecommendedCourse(Long recordId, Long userId, String name) {
        RunningRecord runningRecord = runningRecordService.validateRunningRecord(recordId, userId);

        if (recommendedCourseRepository.existsBySourceRecordId(recordId)) {
            throw new IllegalArgumentException("이미 추천된 기록입니다.");
        }

        String title = name + "님의 추천 코스 #" + recordId;

        RecommendedCourse course = RecommendedCourse.builder()
                .user(runningRecord.getUser())
                .title(title)
                .sourceRecordId(recordId)
                .imageUrl(runningRecord.getImageUrl())
                .startLocationName(runningRecord.getStartLocationName())
                .endLocationName(runningRecord.getEndLocationName())
                .description("")
                .pathGeoJson(runningRecord.getPathGeoJson())
                .totalDistance(runningRecord.getTotalDistance())
                .build();

        recommendedCourseRepository.save(course);
    }

    @Transactional
    public void updateRecommendedCourse(Long courseId, Long userId, RecommendedCourseUpdateRequest request) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (!(recommendedCourse.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("등록한 사용자만 수정이 가능합니다.");
        }

        if (request.title() != null && !request.title().isBlank()) {
            recommendedCourse.changeTitle(request.title());
        }

        if (request.description() != null) {
            recommendedCourse.changeDescription(request.description());
        }
    }

    @Transactional
    public void deleteRecommendedCourse(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (!(recommendedCourse.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("등록한 사용자만 삭제 가능합니다.");
        }


        courseBookmarkRepository.deleteByRecommendedCourseId(recommendedCourse.getId());
        recommendedCourseRepository.delete(recommendedCourse);
    }

    @Transactional(readOnly = true)
    public RecommendedCourseResponse getRecommendedCourseDetail(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        long likeCount = courseLikeRepository.countByRecommendedCourseId(courseId);
        boolean isLiked = courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);

        boolean isBookmarked = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);

        return RecommendedCourseResponse.toCourseResponse(recommendedCourse, likeCount, isLiked, isBookmarked);
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseMyListResponse> getMyRecommendedCourses(Long userId) {
        return recommendedCourseRepository.findWithUserByUserId(userId).stream()
                .map(RecommendedCourseMyListResponse::toCourseListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedCourseListResponse> getRecommendedCourses(SortType sortType, Long userId) {
        List<RecommendedCourse> courses = switch (sortType) {
            case LIKE -> recommendedCourseRepository.findAllOrderedByLikeCount(userId);
            case DISTANCE -> recommendedCourseRepository.findAllOrderedByTotalDistance(userId);
            case RECENT -> recommendedCourseRepository.findAllOrderedByCreatedAt(userId);
        };

        List<Long> bookmarkedCourseIds = courseBookmarkRepository.findRecommendedCourseIdByUserId(userId);

        return courses.stream()
                .map(course -> {
                    boolean isBookmarked = bookmarkedCourseIds.contains(course.getId());
                    return RecommendedCourseListResponse.toCourseListResponse(course, isBookmarked);
                })
                .toList();
    }
}
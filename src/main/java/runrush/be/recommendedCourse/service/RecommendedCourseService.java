package runrush.be.recommendedCourse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
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
import runrush.be.stats.dto.RecommendedCourseDetailStats;
import runrush.be.stats.service.RunningStatsService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendedCourseService {
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final RunningRecordService runningRecordService;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final RunningStatsService runningStatsService;

    @Transactional
    public void createRecommendedCourse(Long recordId, Long userId, String name) {
        RunningRecord runningRecord = runningRecordService.validateRunningRecord(recordId, userId);

        if (recommendedCourseRepository.existsBySourceRecordId(recordId)) {
            throw new BusinessException(ErrorCode.RECOMMENDED_COURSE_ALREADY_EXISTS);
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
        RecommendedCourse recommendedCourse = findRecommendedCourseById(courseId);
        recommendedCourse.validateOwnership(userId);

        if (request.title() != null && !request.title().isBlank()) {
            recommendedCourse.changeTitle(request.title());
        }

        if (request.description() != null) {
            recommendedCourse.changeDescription(request.description());
        }
    }

    @Transactional
    public void deleteRecommendedCourse(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = findRecommendedCourseById(courseId);
        recommendedCourse.validateOwnership(userId);
        
        recommendedCourse.courseDelete();
    }

    @Transactional(readOnly = true)
    public RecommendedCourseResponse getRecommendedCourseDetail(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = findRecommendedCourseByCourseId(courseId);
        
        long likeCount = courseLikeRepository.countByRecommendedCourseId(courseId);
        boolean isLiked = courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);
        boolean isBookmarked = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);

        return RecommendedCourseResponse.toCourseResponse(recommendedCourse, likeCount, isLiked, isBookmarked);
    }

    @Transactional(readOnly = true)
    public RecommendedCourseResponse getRecommendedCourseDetailWithStats(Long courseId, Long userId) {
        RecommendedCourse recommendedCourse = findRecommendedCourseByCourseId(courseId);
        
        long likeCount = courseLikeRepository.countByRecommendedCourseId(courseId);
        boolean isLiked = courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);
        boolean isBookmarked = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);

        RecommendedCourseDetailStats stats =
                runningStatsService.getRecommendedCourseDetailStats(courseId, userId);

        return RecommendedCourseResponse.toCourseResponse(
                recommendedCourse,
                likeCount,
                isLiked,
                isBookmarked,
                stats.totalCompletionCount(),
                stats.uniqueRunnerCount(),
                stats.averageCompletionTimeSeconds(),
                stats.averagePace(),
                stats.myCompletionCount(),
                stats.myBestTimeSeconds(),
                stats.myAveragePace(),
                stats.topRunners()
        );
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

    private RecommendedCourse findRecommendedCourseById(Long courseId) {
        return recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));
    }
    
    private RecommendedCourse findRecommendedCourseByCourseId(Long courseId) {
        return recommendedCourseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));
    }
}
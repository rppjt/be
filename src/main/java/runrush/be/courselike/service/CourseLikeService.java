package runrush.be.courselike.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.courselike.domain.CourseLike;
import runrush.be.courselike.repository.CourseLikeRepository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLikeService {
    private final CourseLikeRepository courseLikeRepository;
    private final UserService userService;
    private final RecommendedCourseRepository recommendedCourseRepository;


    @Transactional
    public void likeToggle(Long userId, Long courseId) {
        User user = userService.findUserById(userId);
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));

        try {
            // DB 제약조건을 활용한 안전한 토글
            Optional<CourseLike> existsLike = courseLikeRepository.findByUserIdAndRecommendedCourseId(userId, courseId);
            if (existsLike.isPresent()) {
                courseLikeRepository.delete(existsLike.get());
                log.info("좋아요 해제: userId={}, courseId={}", userId, courseId);
            } else {
                CourseLike courseLike = CourseLike.builder()
                        .user(user)
                        .recommendedCourse(recommendedCourse)
                        .build();
                courseLikeRepository.save(courseLike);
                log.info("좋아요 추가: userId={}, courseId={}", userId, courseId);
            }
        } catch (DataIntegrityViolationException e) {
            // 동시 좋아요 추가 시도
            log.info("UNIQUE 제약조건 위반 감지: userId={}, courseId={}", userId, courseId);
            handleConcurrentLikeAttempt(userId, courseId);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 동시 수정 시도
            log.info("낙관적 락 충돌 감지: userId={}, courseId={}", userId, courseId);
            handleConcurrentLikeAttempt(userId, courseId);
        }
    }

    private void handleConcurrentLikeAttempt(Long userId, Long courseId) {
        // 현재 상태 확인 후 토글 의도에 맞게 처리
        boolean currentlyLiked = courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);
        
        if (currentlyLiked) {
            // 이미 좋아요가 있으면 제거
            Optional<CourseLike> likeToDelete = courseLikeRepository
                .findByUserIdAndRecommendedCourseIdWithoutLock(userId, courseId);
            if (likeToDelete.isPresent()) {
                courseLikeRepository.delete(likeToDelete.get());
                log.info("동시성 복구 - 좋아요 해제: userId={}, courseId={}", userId, courseId);
            }
        }
        // 좋아요가 없는 경우는 다른 스레드가 이미 추가했으므로 완료로 간주
    }
}
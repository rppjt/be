package runrush.be.coursebookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.coursebookmark.domain.CourseBookmark;
import runrush.be.coursebookmark.dto.BookmarkResponse;
import runrush.be.coursebookmark.dto.BookmarkedCourseListResponse;
import runrush.be.coursebookmark.repository.CourseBookmarkRepository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseBookmarkService {
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final UserService userService;
    private final RecommendedCourseRepository recommendedCourseRepository;

    @Transactional
    public BookmarkResponse setBookmark(Long userId, Long courseId, Boolean isBookmarked) {
        User user = userService.findUserById(userId);
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));

        if (recommendedCourse.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        try {
            // DB 제약조건을 활용한 안전한 토글
            Optional<CourseBookmark> existingBookmark = courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId);

            if (isBookmarked) {
                if (existingBookmark.isEmpty()) {
                    CourseBookmark bookmark = CourseBookmark.builder()
                            .user(user)
                            .course(recommendedCourse)
                            .build();
                    courseBookmarkRepository.save(bookmark);
                    log.info("북마크 추가: userId={}, courseId={}", userId, courseId);
                }
                return BookmarkResponse.bookmarked(courseId);

            } else {
                if (existingBookmark.isPresent()) {
                    courseBookmarkRepository.delete(existingBookmark.get());
                    log.info("북마크 해제: userId={}, courseId={}", userId, courseId);
                }
                return BookmarkResponse.unbookmarked(courseId);
            }
            
        } catch (DataIntegrityViolationException e) {
            // 동시 북마크 추가 시도
            log.info("UNIQUE 제약조건 위반 감지: userId={}, courseId={}", userId, courseId);
            return handleConcurrentBookmarkAttempt(userId, courseId, isBookmarked);
            
        } catch (ObjectOptimisticLockingFailureException e) {
            // 동시 수정 시도
            log.info("낙관적 락 충돌 감지: userId={}, courseId={}", userId, courseId);
            return handleConcurrentBookmarkAttempt(userId, courseId, isBookmarked);
        }
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getBookmarkStatus(Long userId, Long courseId) {
        recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_COURSE_NOT_FOUND));

        boolean isBookmarked = courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId)
                .isPresent();

        return BookmarkResponse.current(courseId, isBookmarked);
    }

    @Transactional(readOnly = true)
    public List<BookmarkedCourseListResponse> getBookmarkedCourses(Long userId) {
        return courseBookmarkRepository.findWithCourseByUserId(userId).stream()
                .map(course -> BookmarkedCourseListResponse.toBookmarkedCourseListResponse(course.getRecommendedCourse(), course.getId()))
                .toList();
    }

    private BookmarkResponse handleConcurrentBookmarkAttempt(Long userId, Long courseId, Boolean isBookmarked) {
        boolean currentlyBookmarked = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, courseId);
        
        if (isBookmarked) {
            // 북마크 추가 요청이었는데 동시성 충돌 발생
            if (currentlyBookmarked) {
                log.info("동시성 복구 - 북마크 이미 존재: userId={}, courseId={}", userId, courseId);
                return BookmarkResponse.bookmarked(courseId);
            } else {
                // 북마크가 없다면 다른 스레드의 추가 실패, 재시도하지 않음
                log.info("동시성 복구 - 북마크 추가 실패: userId={}, courseId={}", userId, courseId);
                return BookmarkResponse.unbookmarked(courseId);
            }
        } else {
            // 북마크 해제 요청이었는데 동시성 충돌 발생
            if (!currentlyBookmarked) {
                log.info("동시성 복구 - 북마크 이미 해제됨: userId={}, courseId={}", userId, courseId);
                return BookmarkResponse.unbookmarked(courseId);
            } else {
                // 북마크가 여전히 있다면 해제 시도
                Optional<CourseBookmark> bookmarkToDelete = courseBookmarkRepository
                    .findByUserIdAndRecommendedCourseIdWithoutLock(userId, courseId);
                if (bookmarkToDelete.isPresent()) {
                    courseBookmarkRepository.delete(bookmarkToDelete.get());
                    log.info("동시성 복구 - 북마크 해제: userId={}, courseId={}", userId, courseId);
                    return BookmarkResponse.unbookmarked(courseId);
                }
                return BookmarkResponse.bookmarked(courseId);
            }
        }
    }
}
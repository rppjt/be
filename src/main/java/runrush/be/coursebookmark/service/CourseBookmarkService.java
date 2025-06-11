package runrush.be.coursebookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "존재하지 않는 코스입니다."));

        if (recommendedCourse.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "본인이 등록한 코스는 즐겨찾기 할 수 없습니다.");
        }

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
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getBookmarkStatus(Long userId, Long courseId) {
        recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "존재하지 않는 코스입니다."));

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
}
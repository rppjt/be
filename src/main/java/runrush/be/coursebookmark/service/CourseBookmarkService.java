package runrush.be.coursebookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.coursebookmark.domain.CourseBookmark;
import runrush.be.coursebookmark.dto.BookmarkToggleResponse;
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
    public BookmarkToggleResponse toggleBookmark(Long userId, Long courseId) {
        User user = userService.findUserById(userId);
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (recommendedCourse.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 등록한 코스는 즐겨찾기 할 수 없습니다.");
        }

        Optional<CourseBookmark> existsBookmark = courseBookmarkRepository.findByUserIdAndCourseId(userId, courseId);
        if (existsBookmark.isPresent()) {
            courseBookmarkRepository.delete(existsBookmark.get());
            log.info("북마크 해제: courseId={}", courseId);

            return BookmarkToggleResponse.bookmarked();
        } else {
            CourseBookmark bookmark = CourseBookmark.builder()
                    .user(user)
                    .course(recommendedCourse)
                    .build();
            courseBookmarkRepository.save(bookmark);
            log.info("북마크 추가: courseId={}", courseId);

            return BookmarkToggleResponse.unbookmarked();
        }
    }

    @Transactional(readOnly = true)
    public List<BookmarkedCourseListResponse> getBookmarkedCourses(Long userId) {
        return courseBookmarkRepository.findByUserId(userId).stream()
                .map(course -> BookmarkedCourseListResponse.toBookmarkedCourseListResponse(course.getRecommendedCourse(), course.getId()))
                .toList();
    }
}
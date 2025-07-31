package runrush.be.coursebookmark.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseBookmarkServiceTest {

    @Mock
    private CourseBookmarkRepository courseBookmarkRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private RecommendedCourseRepository recommendedCourseRepository;
    
    @InjectMocks
    private CourseBookmarkService courseBookmarkService;

    @Test
    @DisplayName("북마크 추가 성공")
    void setBookmark_AddBookmark_Success() {
        Long userId = 1L;
        Long courseId = 1L;
        Boolean isBookmarked = true;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        User courseOwner = mock(User.class);
        when(courseOwner.getId()).thenReturn(2L);

        RecommendedCourse course = mock(RecommendedCourse.class);
        when(course.getUser()).thenReturn(courseOwner);

        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId))
                .thenReturn(Optional.empty());

        BookmarkResponse result = courseBookmarkService.setBookmark(userId, courseId, isBookmarked);

        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.isBookmarked()).isTrue();

        verify(courseBookmarkRepository).save(any(CourseBookmark.class));
    }

    @Test
    @DisplayName("북마크 해제 성공")
    void setBookmark_RemoveBookmark_Success() {
        Long userId = 1L;
        Long courseId = 1L;
        Boolean isBookmarked = false;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        User courseOwner = mock(User.class);
        when(courseOwner.getId()).thenReturn(2L);

        RecommendedCourse course = mock(RecommendedCourse.class);
        when(course.getUser()).thenReturn(courseOwner);

        CourseBookmark bookmark = mock(CourseBookmark.class);

        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId))
                .thenReturn(Optional.of(bookmark));

        BookmarkResponse result = courseBookmarkService.setBookmark(userId, courseId, isBookmarked);

        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.isBookmarked()).isFalse();

        verify(courseBookmarkRepository).delete(any(CourseBookmark.class));
    }

    @Test
    @DisplayName("북마크 설정 실패 - 코스 없음")
    void setBookmark_CourseNotFound() {
        Long userId = 1L;
        Long courseId = 999L;
        Boolean isBookmarked = true;

        User user = mock(User.class);
        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.setBookmark(userId, courseId, isBookmarked))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);

        verify(courseBookmarkRepository, never()).findByUserIdAndRecommendedCourseId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 설정 실패 - 본인 코스")
    void setBookmark_OwnCourse() {
        Long userId = 1L;
        Long courseId = 1L;
        Boolean isBookmarked = true;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        User courseOwner = mock(User.class);
        when(courseOwner.getId()).thenReturn(userId);

        RecommendedCourse course = mock(RecommendedCourse.class);
        when(course.getUser()).thenReturn(courseOwner);

        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseBookmarkService.setBookmark(userId, courseId, isBookmarked))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        verify(courseBookmarkRepository, never()).findByUserIdAndRecommendedCourseId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 상태 조회 성공")
    void getBookmarkStatus_Success() {
        Long userId = 1L;
        Long courseId = 1L;

        RecommendedCourse course = mock(RecommendedCourse.class);
        CourseBookmark bookmark = mock(CourseBookmark.class);

        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId))
                .thenReturn(Optional.of(bookmark));

        BookmarkResponse result = courseBookmarkService.getBookmarkStatus(userId, courseId);

        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.isBookmarked()).isTrue();

        when(courseBookmarkRepository.findByUserIdAndRecommendedCourseId(userId, courseId))
                .thenReturn(Optional.empty());

        BookmarkResponse result2 = courseBookmarkService.getBookmarkStatus(userId, courseId);

        assertThat(result2.courseId()).isEqualTo(courseId);
        assertThat(result2.isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("북마크된 코스 목록 조회 성공")
    void getBookmarkedCourses_Success() {
        Long userId = 1L;

        CourseBookmark bookmark1 = mock(CourseBookmark.class);
        CourseBookmark bookmark2 = mock(CourseBookmark.class);

        when(bookmark1.getId()).thenReturn(1L);
        when(bookmark2.getId()).thenReturn(2L);

        RecommendedCourse course1 = mock(RecommendedCourse.class);
        RecommendedCourse course2 = mock(RecommendedCourse.class);

        when(bookmark1.getRecommendedCourse()).thenReturn(course1);
        when(bookmark2.getRecommendedCourse()).thenReturn(course2);

        List<CourseBookmark> bookmarks = List.of(bookmark1, bookmark2);

        when(courseBookmarkRepository.findWithCourseByUserId(userId))
                .thenReturn(bookmarks);

        List<BookmarkedCourseListResponse> result =
                courseBookmarkService.getBookmarkedCourses(userId);

        assertThat(result).hasSize(2);
        verify(courseBookmarkRepository).findWithCourseByUserId(userId);
    }
}
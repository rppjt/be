package runrush.be.courselike.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.courselike.domain.CourseLike;
import runrush.be.courselike.repository.CourseLikeRepository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseLikeServiceTest {

    @Mock
    private CourseLikeRepository courseLikeRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private RecommendedCourseRepository recommendedCourseRepository;
    
    @InjectMocks
    private CourseLikeService courseLikeService;

    @Test
    @DisplayName("좋아요 추가 성공")
    void likeToggle_AddLike_Success() {
        Long userId = 1L;
        Long courseId = 1L;

        User user = mock(User.class);
        RecommendedCourse course = mock(RecommendedCourse.class);

        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseLikeRepository.findByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(Optional.empty());

        courseLikeService.likeToggle(userId, courseId);

        verify(courseLikeRepository).save(any(CourseLike.class));
        verify(courseLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("좋아요 해제 성공")
    void likeToggle_RemoveLike_Success() {
        Long userId = 1L;
        Long courseId = 1L;

        User user = mock(User.class);
        RecommendedCourse course = mock(RecommendedCourse.class);
        CourseLike existingLike = mock(CourseLike.class);

        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseLikeRepository.findByUserIdAndRecommendedCourseId(userId, courseId)).thenReturn(Optional.of(existingLike));

        courseLikeService.likeToggle(userId, courseId);

        verify(courseLikeRepository).delete(existingLike);
        verify(courseLikeRepository, never()).save(any(CourseLike.class));
    }

    @Test
    @DisplayName("좋아요 토글 실패 - 코스 없음")
    void likeToggle_CourseNotFound() {
        Long userId = 1L;
        Long courseId = 999L;

        User user = mock(User.class);
        when(userService.findUserById(userId)).thenReturn(user);
        when(recommendedCourseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseLikeService.likeToggle(userId, courseId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDED_COURSE_NOT_FOUND);

        verify(courseLikeRepository, never()).findByUserIdAndRecommendedCourseId(anyLong(), anyLong());
        verify(courseLikeRepository, never()).save(any());
        verify(courseLikeRepository, never()).delete(any());
    }
}
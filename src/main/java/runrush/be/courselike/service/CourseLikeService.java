package runrush.be.courselike.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        Optional<CourseLike> existsLike = courseLikeRepository.findByUserIdAndRecommendedCourseId(userId, courseId);
        if (existsLike.isPresent()) {
            courseLikeRepository.delete(existsLike.get());
            log.info("좋아요 해제: courseId={}", courseId);

        } else {
            CourseLike courseLike = CourseLike.builder()
                    .user(user)
                    .recommendedCourse(recommendedCourse)
                    .build();
            courseLikeRepository.save(courseLike);
            log.info("북마크 추가: courseId={}", courseId);
        }
    }
}
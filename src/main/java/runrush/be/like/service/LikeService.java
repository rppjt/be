package runrush.be.like.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.like.domain.Like;
import runrush.be.like.repository.LikeRepository;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final RecommendedCourseRepository recommendedCourseRepository;


    @Transactional
    public void likeToggle(Long userId, Long courseId) {
        User user = userService.findUserById(userId);
        RecommendedCourse recommendedCourse = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        Optional<Like> existsLike = likeRepository.findByUserIdAndRecommendedCourseId(userId, courseId);
        if (existsLike.isPresent()) {
            likeRepository.delete(existsLike.get());
            log.info("좋아요 해제: courseId={}", courseId);

        } else {
            Like like = Like.builder()
                    .user(user)
                    .recommendedCourse(recommendedCourse)
                    .build();
            likeRepository.save(like);
            log.info("북마크 추가: courseId={}", courseId);
        }
    }
}
package runrush.be.courselike.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.user.domain.User;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "course_like")
public class CourseLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "recommended_course_id")
    private RecommendedCourse recommendedCourse;

    @Builder
    public CourseLike(User user, RecommendedCourse recommendedCourse) {
        this.user = user;
        this.recommendedCourse = recommendedCourse;
    }
}
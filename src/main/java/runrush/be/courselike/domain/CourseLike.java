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
@Table(name = "course_like", 
       indexes = {
           @Index(name = "idx_course_like_user_course", columnList = "user_id, recommended_course_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_course_like_user_course",
               columnNames = {"user_id", "recommended_course_id"}
           )
       })
public class CourseLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_course_id")
    private RecommendedCourse recommendedCourse;

    @Version
    private Long version;

    @Builder
    public CourseLike(User user, RecommendedCourse recommendedCourse) {
        this.user = user;
        this.recommendedCourse = recommendedCourse;
    }
}
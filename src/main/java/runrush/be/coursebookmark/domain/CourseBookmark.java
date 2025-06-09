package runrush.be.coursebookmark.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course_bookmark")
public class CourseBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_course_id")
    private RecommendedCourse recommendedCourse;

    @CreatedDate
    @Column(name = "bookmarked_at")
    private LocalDateTime bookmarkedAt;

    @Builder
    public CourseBookmark(User user, RecommendedCourse course) {
        this.user = user;
        this.recommendedCourse = course;
    }
}

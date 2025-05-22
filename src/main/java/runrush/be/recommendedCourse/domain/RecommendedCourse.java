package runrush.be.recommendedCourse.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "recommended_course")
public class RecommendedCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "running_record_id")
    @OneToOne(fetch = FetchType.LAZY)
    private RunningRecord runningRecord;

    private String title;

    private String description;

    @Lob
    @Column(name = "path_geo_json")
    private String pathGeoJson;

    @Column(name = "total_distance")
    private double totalDistance;

    private double pace;

    private double latitude;

    private double longitude;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RecommendedCourse(User user,
                             RunningRecord runningRecord,
                             String title,
                             String description,
                             String pathGeoJson,
                             double totalDistance,
                             double pace,
                             double latitude,
                             double longitude) {
        this.user = user;
        this.runningRecord = runningRecord;
        this.title = title;
        this.description = description;
        this.pathGeoJson = pathGeoJson;
        this.totalDistance = totalDistance;
        this.pace = pace;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }
}
package runrush.be.recommendedCourse.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
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

    @Column(name = "source_record_id", unique = true)
    private Long sourceRecordId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "start_location_name")
    private String startLocationName;

    @Column(name = "end_location_name")
    private String endLocationName;

    private String title;

    private String description;

    @Lob
    @Column(name = "path_geo_json", columnDefinition = "LONGTEXT")
    private String pathGeoJson;

    @Column(name = "total_distance")
    private double totalDistance;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public RecommendedCourse(User user,
                             Long sourceRecordId,
                             String imageUrl,
                             String startLocationName,
                             String endLocationName,
                             String title,
                             String description,
                             String pathGeoJson,
                             double totalDistance) {
        this.user = user;
        this.sourceRecordId = sourceRecordId;
        this.imageUrl = imageUrl;
        this.startLocationName = startLocationName;
        this.endLocationName = endLocationName;
        this.title = title;
        this.description = description;
        this.pathGeoJson = pathGeoJson;
        this.totalDistance = totalDistance;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void courseDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}
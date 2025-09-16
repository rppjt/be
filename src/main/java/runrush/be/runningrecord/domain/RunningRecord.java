package runrush.be.runningrecord.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "running_record", indexes = {
    @Index(name = "idx_user_id_is_deleted", columnList = "user_id, is_deleted"),
    @Index(name = "idx_user_id_started_time", columnList = "user_id, started_time")
})
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_course_id")
    private RecommendedCourse recommendedCourse;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "start_location_name")
    private String startLocationName;

    @Column(name = "end_location_name")
    private String endLocationName;

    @Lob
    @Column(name = "path_geo_json", columnDefinition = "LONGTEXT")
    private String pathGeoJson;

    @Column(name = "total_distance")
    private double totalDistance;

    @Column(name = "start_latitude")
    private double startLatitude;

    @Column(name = "start_longitude")
    private double startLongitude;

    @Column(name = "end_latitude")
    private double endLatitude;

    @Column(name = "end_longitude")
    private double endLongitude;

    @Column(name = "started_time")
    private LocalDateTime startedTime;

    @Column(name = "ended_time")
    private LocalDateTime endedTime;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "total_time")
    private long totalTime;

    private double pace;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    // 멱등성을 위한 요청 식별자 (클라이언트에서 제공하는 고유 키)
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Builder
    public RunningRecord(
            User user,
            RecommendedCourse recommendedCourse,
            String imageUrl,
            String startLocationName,
            String endLocationName,
            String pathGeoJson,
            double totalDistance,
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            LocalDateTime startedTime,
            LocalDateTime endedTime,
            long totalTime,
            double pace,
            String idempotencyKey
    ) {
        this.user = user;
        this.recommendedCourse = recommendedCourse;
        this.imageUrl = imageUrl;
        this.startLocationName = startLocationName;
        this.endLocationName = endLocationName;
        this.pathGeoJson = pathGeoJson;
        this.totalDistance = totalDistance;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.startedTime = startedTime;
        this.endedTime = endedTime;
        this.totalTime = totalTime;
        this.pace = pace;
        this.idempotencyKey = idempotencyKey;
    }

    public void recordDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public boolean isRecordDeleted() {
        return isDeleted;
    }
}

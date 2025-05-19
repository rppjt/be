package runrush.be.runningrecord.domain;

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
@Table(name = "running_record")
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    @Column(name = "path_geo_json", columnDefinition = "TEXT")
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

    @Builder
    public RunningRecord(
            User user,
            String pathGeoJson,
            double totalDistance,
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            LocalDateTime startedTime,
            LocalDateTime endedTime
    ) {
        this.user = user;
        this.pathGeoJson = pathGeoJson;
        this.totalDistance = totalDistance;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.startedTime = startedTime;
        this.endedTime = endedTime;
    }
}

package runrush.be.location.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import runrush.be.common.util.GeoUtils;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_location", indexes = {
    @Index(name = "idx_sharing_updated", columnList = "is_sharing, updated_at")
})
public class UserLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_sharing")
    private Boolean isSharing;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserLocation(User user, Double latitude, Double longitude, Boolean isSharing) {
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isSharing = isSharing != null ? isSharing : true;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void toggleSharing(Boolean isSharing) {
        this.isSharing = isSharing;
    }

    public double calculateDistance(Double targetLat, Double targetLng) {
        return GeoUtils.calculateDistance(this.latitude, this.longitude, targetLat, targetLng);
    }

    public boolean hasSignificantMovement(Double newLat, Double newLng) {
        return GeoUtils.hasSignificantMovement(this.latitude, this.longitude, newLat, newLng);
    }
}
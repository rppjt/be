package runrush.be.location.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;
import runrush.be.common.util.GeoUtils;
import runrush.be.user.domain.User;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Table(name = "user_location")
public class UserLocation {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_sharing")
    private Boolean isSharing;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserLocation(User user, Double latitude, Double longitude, Boolean isSharing) {
        this.user = user;
        this.userId = user.getId();
        this.latitude = latitude;
        this.longitude = longitude;
        this.isSharing = isSharing != null ? isSharing : true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleSharing(Boolean isSharing) {
        this.isSharing = isSharing;
        this.updatedAt = LocalDateTime.now();
    }

    public double calculateDistance(Double targetLat, Double targetLng) {
        return GeoUtils.calculateDistance(this.latitude, this.longitude, targetLat, targetLng);
    }

    public boolean hasSignificantMovement(Double newLat, Double newLng) {
        return GeoUtils.hasSignificantMovement(this.latitude, this.longitude, newLat, newLng);
    }
}
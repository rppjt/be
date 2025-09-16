package runrush.be.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.location.domain.UserLocation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    Optional<UserLocation> findByUserId(Long userId);

    @Query("SELECT ul FROM UserLocation ul " +
            "JOIN FETCH ul.user u " +
            "WHERE ul.isSharing = true " +
            "AND ul.updatedAt > :cutoffTime " +
            "AND ul.user.id != :userId " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Friends f " +
            "    WHERE ((f.requester.id = :userId AND f.target.id = ul.user.id) " +
            "           OR (f.requester.id = ul.user.id AND f.target.id = :userId)) " +
            "    AND f.status = 'ACCEPTED'" +
            ")")
    List<UserLocation> findFriendsWithActiveLocation(@Param("userId") Long userId,
                                                     @Param("cutoffTime") LocalDateTime cutoffTime);
}
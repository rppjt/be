package runrush.be.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.location.domain.UserLocation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    @Query("SELECT ul FROM UserLocation ul " +
            "JOIN FETCH ul.user u " +
            "WHERE ul.isSharing = true " +
            "AND ul.updatedAt > :cutoffTime " +
            "AND ul.userId != :userId " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Friends f " +
            "    WHERE ((f.requester.id = :userId AND f.target.id = ul.userId) " +
            "           OR (f.requester.id = ul.userId AND f.target.id = :userId)) " +
            "    AND f.status = 'ACCEPTED'" +
            ")")
    List<UserLocation> findFriendsWithActiveLocation(@Param("userId") Long userId,
                                                     @Param("cutoffTime") LocalDateTime cutoffTime);
}
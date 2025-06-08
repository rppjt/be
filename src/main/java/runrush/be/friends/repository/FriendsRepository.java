package runrush.be.friends.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import runrush.be.friends.domain.Friends;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendsRepository extends JpaRepository<Friends, Long> {
    // 특정 사용자 간의 관계 조회 (한 방향)
    Optional<Friends> findByRequesterIdAndTargetId(Long requestId, Long targetId);

    // 내가 보낸 친구 요청들
    @Query("SELECT f FROM Friends f " +
            "JOIN FETCH f.target " +
            "WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    List<Friends> findFriendsRequest(@Param("userId") Long userId);


    @Query("SELECT f FROM Friends f " +
            "JOIN FETCH f.requester " +
            "WHERE f.target.id = :userId AND f.status = 'PENDING'")
    List<Friends> findFriendsTarget(@Param("userId") Long userId);

    @Query("SELECT f FROM Friends f " +
            "JOIN FETCH f.target " +
            "WHERE f.requester.id = :userId AND f.status = 'ACCEPTED'")
    List<Friends> findMyFriends(@Param("userId") Long userId);

    // 친구 요청 거절
    void deleteFriendsRequest(Long userId, Long friendId);

    @Modifying
    @Query("DELETE FROM Friends f WHERE " +
            "(f.requester.id = :userId1 AND f.target.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.target.id = :userId1)")
    void deleteAllFriends(@Param("userId1") Long userId1,
                       @Param("userId2") Long userId2);
}
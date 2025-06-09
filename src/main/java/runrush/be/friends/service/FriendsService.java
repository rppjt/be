package runrush.be.friends.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.friends.domain.FriendStatus;
import runrush.be.friends.domain.Friends;
import runrush.be.friends.repository.FriendsRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendsService {
    private final FriendsRepository friendsRepository;
    private final UserService userService;

    @Transactional
    public void sendFriendRequest(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신은 친구 요청을 보낼 수 없습니다.");
        }

        User targetUser = userService.findUserById(targetId);
        User requesterUser = userService.findUserById(requesterId);

        if (hasRelation(requesterId, targetId)) {
            throw new IllegalArgumentException("이미 친구이거나 친구 요청이 존재합니다.");
        }

        Friends friends = Friends.builder()
                .requester(requesterUser)
                .target(targetUser)
                .build();

        friendsRepository.save(friends);
        log.info("친구 요청 전송: {} -> {}", requesterId, targetId);
    }

    @Transactional
    public void acceptFriendRequest(Long requesterId, Long targetId) {
        Friends pendingRequest = friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다."));

        if (pendingRequest.getStatus() != FriendStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 친구 요청이 아닙니다.");
        }

        User target = userService.findUserById(targetId);
        User requester = userService.findUserById(requesterId);

        friendsRepository.delete(pendingRequest);

        Friends friend1 = Friends.builder()
                .requester(requester)
                .target(target)
                .build();
        friend1.accept();

        Friends friend2 = Friends.builder()
                .requester(target)
                .target(requester)
                .build();
        friend2.accept();

        friendsRepository.save(friend1);
        friendsRepository.save(friend2);

        log.info("친구 요청 수락: {} <-> {}", targetId, requesterId);
    }

    @Transactional
    public void rejectFriendRequest(Long requesterId, Long targetId) {
        Friends pendingRequest = friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다."));

        if (pendingRequest.getStatus() != FriendStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 친구 요청이 아닙니다.");
        }

        friendsRepository.delete(pendingRequest);
        log.info("친구 요청 거절: {} -> {}", requesterId, targetId);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        boolean isFriend = friendsRepository.findByRequesterIdAndTargetId(userId, friendId)
                .map(relation -> relation.getStatus() == FriendStatus.ACCEPTED)
                .orElse(false);

        if (!isFriend) {
            throw new IllegalArgumentException("친구 관계가 아닙니다.");
        }

        friendsRepository.deleteAllFriends(userId, friendId);
        log.info("친구 관계 해제: {} <-> {}", userId, friendId);
    }

    private boolean hasRelation(Long userId, Long friendId) {
        return friendsRepository.findByRequesterIdAndTargetId(userId, friendId).isPresent() ||
                friendsRepository.findByRequesterIdAndTargetId(friendId, userId).isPresent();
    }
}
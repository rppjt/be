package runrush.be.friends.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.friends.domain.FriendStatus;
import runrush.be.friends.domain.Friends;
import runrush.be.friends.dto.FriendResponse;
import runrush.be.friends.dto.ReceivedFriendRequestResponse;
import runrush.be.friends.dto.SentFriendRequestResponse;
import runrush.be.friends.repository.FriendsRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendsService {
    private final FriendsRepository friendsRepository;
    private final UserService userService;

    @Transactional
    public void sendFriendRequest(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_FRIEND);
        }

        User targetUser = userService.findUserById(targetId);
        User requesterUser = userService.findUserById(requesterId);

        try {
            // 안전한 관계 확인 (동시성 고려)
            if (hasRelationSafe(requesterId, targetId)) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
            }

            Friends friends = Friends.builder()
                    .requester(requesterUser)
                    .target(targetUser)
                    .build();

            friendsRepository.save(friends);
            log.info("친구 요청 전송: {} -> {}", requesterId, targetId);
            
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약조건 위반 = 동시 친구 요청
            log.info("동시 친구 요청 감지: {} -> {}", requesterId, targetId);
            handleConcurrentFriendRequest(requesterId, targetId);
        }
    }

    @Transactional
    public void acceptFriendRequest(Long requesterId, Long targetId) {
        try {
            // 비관적 락으로 요청 조회 (동시 수락 방지)
            Friends pendingRequest = friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

            if (pendingRequest.getStatus() != FriendStatus.PENDING) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);
            }

            User target = userService.findUserById(targetId);
            User requester = userService.findUserById(requesterId);

            // 요청 삭제와 양방향 관계 생성을 하나의 트랜잭션에서 처리
            friendsRepository.delete(pendingRequest);

            // 양방향 친구 관계 생성 (정렬된 순서로 생성하여 데드락 방지)
            createBidirectionalFriendship(requester, target);

            log.info("친구 요청 수락: {} <-> {}", targetId, requesterId);
            
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("친구 요청 수락 중 동시성 충돌: {} -> {}", requesterId, targetId);
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);
        } catch (DataIntegrityViolationException e) {
            log.info("친구 관계 생성 중 제약조건 위반: {} <-> {}", requesterId, targetId);
            // 이미 친구 관계가 존재하는 것으로 처리
        }
    }

    @Transactional
    public void rejectFriendRequest(Long requesterId, Long targetId) {
        Friends pendingRequest = friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (pendingRequest.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);
        }

        friendsRepository.deleteFriendsRequest(requesterId, targetId);
        log.info("친구 요청 거절: {} -> {}", requesterId, targetId);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        boolean isFriend = friendsRepository.findByRequesterIdAndTargetId(userId, friendId)
                .map(relation -> relation.getStatus() == FriendStatus.ACCEPTED)
                .orElse(false);

        if (!isFriend) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
        }

        friendsRepository.deleteAllFriends(userId, friendId);
        log.info("친구 관계 해제: {} <-> {}", userId, friendId);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getMyFriends(Long userId) {
        List<Friends> myFriends = friendsRepository.findMyFriends(userId);

        return myFriends.stream()
                .map(friends -> new FriendResponse(
                        friends.getTarget().getId(),
                        friends.getTarget().getName(),
                        friends.getTarget().getProfileImage()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SentFriendRequestResponse> getSentFriendRequest(Long userId) {
        List<Friends> friendsRequest = friendsRepository.findFriendsRequest(userId);

        return friendsRequest.stream()
                .map(request -> new SentFriendRequestResponse(
                        request.getTarget().getId(),
                        request.getTarget().getName(),
                        request.getTarget().getProfileImage()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivedFriendRequestResponse> getReceivedFriendRequest(Long userId) {
        List<Friends> friendsTarget = friendsRepository.findFriendsTarget(userId);

        return friendsTarget.stream()
                .map(request -> new ReceivedFriendRequestResponse(
                        request.getRequester().getId(),
                        request.getRequester().getName(),
                        request.getRequester().getProfileImage()
                ))
                .toList();
    }

    private boolean hasRelation(Long userId, Long friendId) {
        return friendsRepository.findByRequesterIdAndTargetIdWithoutLock(userId, friendId).isPresent() ||
                friendsRepository.findByRequesterIdAndTargetIdWithoutLock(friendId, userId).isPresent();
    }
    
    private boolean hasRelationSafe(Long userId, Long friendId) {
        // 동시성을 고려한 관계 확인 (비관적 락 사용)
        return friendsRepository.findByRequesterIdAndTargetId(userId, friendId).isPresent() ||
                friendsRepository.findByRequesterIdAndTargetId(friendId, userId).isPresent();
    }
    
    private void handleConcurrentFriendRequest(Long requesterId, Long targetId) {
        // 동시 친구 요청 발생 시 처리
        log.info("동시 친구 요청 처리: {} -> {}", requesterId, targetId);
        
        // 현재 상태 확인
        boolean relationExists = hasRelation(requesterId, targetId);
        if (!relationExists) {
            // 관계가 없다면 다른 스레드의 요청이 실패했을 수 있으므로 재시도하지 않음
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }
        
        // 이미 관계가 존재하므로 요청 성공으로 간주
        log.info("친구 요청이 이미 존재함: {} -> {}", requesterId, targetId);
    }
    
    private void createBidirectionalFriendship(User requester, User target) {
        // 데드락 방지를 위해 ID 순서로 정렬하여 생성
        Long smallerId = Math.min(requester.getId(), target.getId());
        Long largerId = Math.max(requester.getId(), target.getId());
        
        User smallerUser = requester.getId().equals(smallerId) ? requester : target;
        User largerUser = requester.getId().equals(largerId) ? requester : target;
        
        // 작은 ID -> 큰 ID 순서로 먼저 생성
        Friends friend1 = Friends.builder()
                .requester(smallerUser)
                .target(largerUser)
                .build();
        friend1.accept();
        friendsRepository.save(friend1);
        
        // 큰 ID -> 작은 ID 순서로 생성
        Friends friend2 = Friends.builder()
                .requester(largerUser)
                .target(smallerUser)
                .build();
        friend2.accept();
        friendsRepository.save(friend2);
    }
}
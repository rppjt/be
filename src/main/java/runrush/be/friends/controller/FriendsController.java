package runrush.be.friends.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.friends.dto.FriendInfoResponse;
import runrush.be.friends.service.FriendsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendsController {
    private final FriendsService friendsService;

    @PostMapping("/request")
    public ResponseEntity<Void> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam Long targetId) {
        friendsService.sendFriendRequest(user.getId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/request/accept/{requesterId}")
    public ResponseEntity<Void> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long requesterId) {
        friendsService.acceptFriendRequest(user.getId(), requesterId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request/reject/{requesterId}")
    public ResponseEntity<Void> rejectFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long requesterId) {
        friendsService.rejectFriendRequest(user.getId(), requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long friendId) {
        friendsService.removeFriend(user.getId(), friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendInfoResponse>> getMyFriends(
            @AuthenticationPrincipal UserPrincipal user) {
        List<FriendInfoResponse> myFriends = friendsService.getMyFriends(user.getId());
        return ResponseEntity.ok().body(myFriends);
    }

    @GetMapping("/request/sent")
    public ResponseEntity<List<FriendInfoResponse>> getSentMyRequests(
            @AuthenticationPrincipal UserPrincipal user) {
        List<FriendInfoResponse> sentFriendRequest = friendsService.getSentFriendRequest(user.getId());
        return ResponseEntity.ok().body(sentFriendRequest);
    }

    @GetMapping("/request/received")
    public ResponseEntity<List<FriendInfoResponse>> getReceivedMyRequests(
            @AuthenticationPrincipal UserPrincipal user) {
        List<FriendInfoResponse> receivedFriendRequest = friendsService.getReceivedFriendRequest(user.getId());
        return ResponseEntity.ok().body(receivedFriendRequest);
    }
}
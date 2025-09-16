package runrush.be.friends.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.friends.dto.FriendResponse;
import runrush.be.friends.dto.ReceivedFriendRequestResponse;
import runrush.be.friends.dto.SentFriendRequestResponse;
import runrush.be.friends.service.FriendsService;

import java.util.List;

@Tag(name = "Friends", description = "친구 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendsController {
    private final FriendsService friendsService;

    @Operation(summary = "친구 요청 전송", description = "다른 사용자에게 친구 요청을 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "친구 요청 전송 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 친구이거나 요청이 존재함")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/request")
    public ResponseEntity<Void> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "친구 요청 대상 사용자 ID") @RequestParam Long targetId) {
        friendsService.sendFriendRequest(user.getId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구 요청 수락 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "친구 요청을 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/request/accept/{requesterId}")
    public ResponseEntity<Void> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "친구 요청자 ID") @PathVariable Long requesterId) {
        friendsService.acceptFriendRequest(user.getId(), requesterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "친구 요청 거절 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "친구 요청을 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/request/reject/{requesterId}")
    public ResponseEntity<Void> rejectFriendRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "친구 요청자 ID") @PathVariable Long requesterId) {
        friendsService.rejectFriendRequest(user.getId(), requesterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "친구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "친구 관계를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "삭제할 친구 ID") @PathVariable Long friendId) {
        friendsService.removeFriend(user.getId(), friendId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 친구 목록 조회", description = "내 친구 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getMyFriends(
            @AuthenticationPrincipal UserPrincipal user) {
        List<FriendResponse> myFriends = friendsService.getMyFriends(user.getId());
        return ResponseEntity.ok(myFriends);
    }

    @Operation(summary = "보낸 친구 요청 목록 조회", description = "내가 보낸 친구 요청 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "보낸 친구 요청 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/request/sent")
    public ResponseEntity<List<SentFriendRequestResponse>> getSentMyRequests(
            @AuthenticationPrincipal UserPrincipal user) {
        List<SentFriendRequestResponse> sentFriendRequest = friendsService.getSentFriendRequest(user.getId());
        return ResponseEntity.ok(sentFriendRequest);
    }

    @Operation(summary = "받은 친구 요청 목록 조회", description = "내가 받은 친구 요청 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "받은 친구 요청 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/request/received")
    public ResponseEntity<List<ReceivedFriendRequestResponse>> getReceivedMyRequests(
            @AuthenticationPrincipal UserPrincipal user) {
        List<ReceivedFriendRequestResponse> receivedFriendRequest = friendsService.getReceivedFriendRequest(user.getId());
        return ResponseEntity.ok(receivedFriendRequest);
    }
}
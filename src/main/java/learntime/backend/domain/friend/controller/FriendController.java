package learntime.backend.domain.friend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.friend.dto.response.FriendRequestResponseDTO;
import learntime.backend.domain.friend.dto.response.FriendResponseDTO;
import learntime.backend.domain.friend.service.FriendService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/friends")
@RequiredArgsConstructor
@Tag(name = "친구 API", description = "친구 요청, 승인/거절, 친구 목록 관리를 위한 API (JWT 필요)")
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    @Operation(summary = "친구 목록 조회", description = "사용자의 친구 목록을 조회합니다.")
    public ResponseEntity<List<FriendResponseDTO>> getFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(friendService.getFriends(userDetails.userId()));
    }

    @GetMapping("/requests/received")
    @Operation(summary = "받은 친구 요청 목록 조회", description = "사용자가 받은 대기 중인 친구 요청을 조회합니다.")
    public ResponseEntity<List<FriendRequestResponseDTO>> getReceivedPendingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(friendService.getReceivedPendingRequests(userDetails.userId()));
    }

    @GetMapping("/requests/sent")
    @Operation(summary = "보낸 친구 요청 목록 조회", description = "사용자가 보낸 대기 중인 친구 요청을 조회합니다.")
    public ResponseEntity<List<FriendRequestResponseDTO>> getSentPendingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(friendService.getSentPendingRequests(userDetails.userId()));
    }

    @DeleteMapping("/{friendUserId}")
    @Operation(summary = "친구 삭제", description = "사용자와 특정 사용자 사이의 친구 관계를 삭제합니다.")
    public ResponseEntity<Void> deleteFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendUserId) {
        friendService.deleteFriend(userDetails.userId(), friendUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{receiverId}")
    @Operation(summary = "친구 요청 보내기", description = "상대방에게 친구 요청을 보냅니다. 상대방이 승인해야 친구가 됩니다.")
    public ResponseEntity<Long> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long receiverId) {
        Long friendRequestId = friendService.sendFriendRequest(userDetails.userId(), receiverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(friendRequestId);
    }

    @PatchMapping("/requests/{friendRequestId}/accept")
    @Operation(summary = "친구 요청 승인", description = "받은 친구 요청을 승인하고 친구 관계를 생성합니다.")
    public ResponseEntity<Long> acceptFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendRequestId) {
        Long friendId = friendService.acceptFriendRequest(userDetails.userId(), friendRequestId);
        return ResponseEntity.ok(friendId);
    }

    @PatchMapping("/requests/{friendRequestId}/reject")
    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    public ResponseEntity<Void> rejectFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendRequestId) {
        friendService.rejectFriendRequest(userDetails.userId(), friendRequestId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/requests/{friendRequestId}")
    @Operation(summary = "친구 요청 취소", description = "본인이 보낸 대기 중인 친구 요청을 취소합니다.")
    public ResponseEntity<Void> cancelFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendRequestId) {
        friendService.cancelFriendRequest(userDetails.userId(), friendRequestId);
        return ResponseEntity.noContent().build();
    }

}

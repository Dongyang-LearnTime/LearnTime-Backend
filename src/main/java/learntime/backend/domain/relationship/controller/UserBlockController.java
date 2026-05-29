package learntime.backend.domain.relationship.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.relationship.service.UserBlockService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/block")
@RequiredArgsConstructor
@Tag(name = "사용자 차단 API", description = "사용자 차단, 차단 사용자 목록 관리를 위한 API (JWT 필요)")
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping("/{blockedId}")
    @Operation(
            summary = "사용자 차단",
            description = "특정 사용자의 userId를 Path Variable로 받아 차단합니다 (친구 관계에선 불가능)" +
                    "차단 당하면 공부 초대, 쪽지, 댓글 달기가 불가능합니다."
    )
    public ResponseEntity<Void> blockTargetUser(@PathVariable Long blockedId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {

        userBlockService.blockUser(userDetails.userId(), blockedId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{blockedId}")
    @Operation(
            summary = "사용자 차단 해제",
            description = "특정 사용자의 userId를 Path Variable로 받아 UserBlock를 삭제합니다. (차단 해제)"
    )
    public ResponseEntity<Void> unblockTargetUser(@PathVariable Long blockedId,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        userBlockService.unblockUser(userDetails.userId(), blockedId);
        return ResponseEntity.noContent().build();
    }

}

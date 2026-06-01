package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.user.dto.response.BadgeTierInfoResponseDTO;
import learntime.backend.domain.user.dto.response.RecentActivityResponseDTO;
import learntime.backend.domain.user.dto.response.UserSummaryResponseDTO;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.global.dto.CursorResponse;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자 정보 API", description = "사용자의 배지, 포인트, 티어 및 최근 학습 활동 조회 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/summary")
    @Operation(summary = "나의 요약 정보 조회", description = "현재 로그인한 사용자의 배지, 포인트, 티어를 조회합니다.")
    public ResponseEntity<UserSummaryResponseDTO> getUserSummary(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getUserSummary(user.getUsername()));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "최근 학습 활동 조회", description = "사용자의 최근 학습 활동(필기, 퀴즈, AI 피드백 중 최근 3개)을 조회합니다.")
    public ResponseEntity<List<RecentActivityResponseDTO>> getRecentActivities(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getRecentActivities(user.getUsername()));
    }

    @GetMapping("/badge-tier-info")
    @Operation(summary = "배지 및 티어 전체 정보 조회", description = "전체 배지와 티어 목록, 그리고 현재 로그인한 사용자의 티어 및 획득한 배지 상태를 반환합니다.")
    public ResponseEntity<BadgeTierInfoResponseDTO> getBadgeTierInfo(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getBadgeTierInfo(user.getUsername()));
    }

    @GetMapping("/search")
    @Operation(summary = "사용자 이름 검색", description = "사용자 이름을 부분 일치로 검색하여 일치하는 사용자 ID 목록을 반환합니다. (커서 기반 페이징)")
    public ResponseEntity<CursorResponse<Long>> searchUsersByName(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "lastUserId", required = false) Long lastUserId,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.searchUserIdsByName(keyword, lastUserId, size));
    }
}

package learntime.backend.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.notification.dto.response.NotificationResponseDTO;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 API", description = "실시간 알림 구독, 알림 목록 조회, 읽음 처리를 관리하는 API (JWT 필요)")
public class SseController {

    private final NotificationService notificationService;

    // 실시간 알림 연결 SSE
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 구독 연결", description = "SSE를 사용하여 새로고침 없이 실시간 알림 발송")
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUserId());
    }

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다.")
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.getUserId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 수 조회", description = "현재 로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userDetails.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "현재 로그인한 사용자의 알림 하나를 읽음 처리합니다.")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.readNotification(userDetails.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "현재 로그인한 사용자의 모든 알림을 읽음 처리합니다.")
    public ResponseEntity<Void> readAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.readAllNotifications(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

}

package learntime.backend.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.notification.dto.response.NotificationCountResponseDTO;
import learntime.backend.domain.notification.dto.response.NotificationResponseDTO;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.global.dto.CursorResponse;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
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
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 커서 기반으로 조회합니다.")
    public ResponseEntity<CursorResponse<NotificationResponseDTO>> getNotifications(
            @RequestParam(required = false) Long cursorId, // 마지막 notificationId
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<NotificationResponseDTO> notifications = notificationService
                .getNotifications(userDetails.getUserId(), cursorId);

        boolean hasNext = notifications.size() == 20; // Repository Top20 limit
        Long nextCursor = notifications.isEmpty() ? null : notifications.getLast().notificationId();

        return ResponseEntity.ok(CursorResponse.of(notifications, nextCursor, hasNext));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 수 조회", description = "사용자의 읽지 않은 알림 개수를 조회합니다.")
    public ResponseEntity<NotificationCountResponseDTO> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long alarmCount = notificationService.getUnreadCount(userDetails.getUserId());
        return ResponseEntity.ok(new NotificationCountResponseDTO(alarmCount));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "사용자의 알림 하나를 읽음 처리합니다.")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.readNotification(userDetails.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 처리합니다.")
    public ResponseEntity<Void> readAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.readAllNotifications(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "단일 알림 삭제 처리", description = "사용자의 알림을 삭제 처리합니다.")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotification(notificationId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "여러 알림 삭제 처리", description = "사용자의 여러 알림을 한 번에 삭제 처리합니다. (벌크 연산으로 N+1 방지)")
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam List<Long> notificationIds) {
        notificationService.deleteNotifications(userDetails.getUserId(), notificationIds);
        return ResponseEntity.noContent().build();
    }

    // --- 프론트엔드 연동 테스트용 API ---
    @PostMapping("/test-send")
    @Operation(summary = "SSE 테스트 알림 발송 (테스트용)", description = "현재 로그인한 사용자 본인에게 테스트 SSE 이벤트를 강제로 발송하여 연결을 확인합니다.")
    public ResponseEntity<String> sendTestNotification(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String testMessage = "서버 테스트 메시지입니다. 발송 시간: " + LocalDateTime.now();
        
        // DB에 저장하지 않고 현재 연결된 SSE로만 이벤트를 쏩니다 (이름: "test-event")
        notificationService.send(userDetails.getUserId(), testMessage, "test-event");
        
        return ResponseEntity.ok("테스트 알림 발송 성공: " + testMessage);
    }
}

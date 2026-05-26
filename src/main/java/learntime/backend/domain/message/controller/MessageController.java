package learntime.backend.domain.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.message.dto.request.MessageReadRequestDTO;
import learntime.backend.domain.message.dto.request.MessageRequestDTO;
import learntime.backend.domain.message.dto.response.MessageResponseDTO;
import learntime.backend.domain.message.service.MessageService;
import learntime.backend.global.security.CustomUserDetails;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "쪽지 API", description = "쪽지 보내기, 보낸/받은 쪽지함 조회, 삭제 기능을 제공합니다. (JWT 필요)")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "쪽지 보내기", description = "특정 사용자에게 쪽지를 발송합니다.")
    public ResponseEntity<Long> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MessageRequestDTO request
    ) {
        Long messageId = messageService.sendMessage(userDetails.userId(), request);
        return ResponseEntity.ok(messageId);
    }

    @GetMapping("/sent")
    @Operation(summary = "보낸 쪽지 목록 조회", description = "내가 보낸 쪽지 목록을 조회합니다. (오프셋 페이징, 기본 10개)")
    public ResponseEntity<PageResponse<MessageResponseDTO>> getSentMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageResponseDTO> response = messageService.getSentMessages(userDetails.userId(), pageable);
        return ResponseEntity.ok(PageResponse.of(response));
    }

    @GetMapping("/received")
    @Operation(summary = "받은 쪽지 목록 조회", description = "내가 받은 쪽지 목록을 조회합니다. (오프셋 페이징, 기본 10개)")
    public ResponseEntity<PageResponse<MessageResponseDTO>> getReceivedMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageResponseDTO> response = messageService.getReceivedMessages(userDetails.userId(), pageable);
        return ResponseEntity.ok(PageResponse.of(response));
    }

    @PatchMapping("/read")
    @Operation(summary = "쪽지 일괄 읽음 처리", description = "수신한 쪽지 목록을 일괄 읽음(readAt 설정) 상태로 변경합니다.")
    public ResponseEntity<Void> readMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MessageReadRequestDTO request
    ) {
        messageService.readMessages(userDetails.userId(), request.messageIds());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "쪽지 삭제", description = "쪽지를 삭제 상태로 변경합니다. 양방향 모두에서 삭제 처리될 경우 DB에서 영구 삭제됩니다.")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long messageId
    ) {
        messageService.deleteMessage(userDetails.userId(), messageId);
        return ResponseEntity.ok().build();
    }
}

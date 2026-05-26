package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.service.core.CommentService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import learntime.backend.domain.community.dto.request.CommentCreateRequestDTO;
import learntime.backend.domain.community.dto.request.CommentUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import learntime.backend.global.dto.CursorResponse;

@RestController
@RequestMapping("/api/community/comment")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 댓글 API", description = "커뮤니티 댓글 CRUD 입니다. (조회 제외 JWT 필요)")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{postId}")
    @Operation(summary = "댓글 목록 조회", description = "댓글 목록을 페이징(커서 기반)으로 가져옵니다.")
    public ResponseEntity<CursorResponse<CommentResponseDTO>> getPost(
            @PathVariable Long postId,
            @RequestParam(required = false) Long lastCommentId,
            @RequestParam(defaultValue = "10") int size) {

        List<CommentResponseDTO> response =
                commentService.getCommentsByPostId(postId, lastCommentId, size);

        boolean hasNext = response.size() == size;
        Long nextCursor = response.isEmpty() ? null : response.get(response.size() - 1).commentId();

        return ResponseEntity.ok(CursorResponse.of(response, nextCursor, hasNext));
    }

    @PostMapping
    @Operation(summary = "댓글 생성", description = "새로운 댓글을 작성합니다.")
    public ResponseEntity<Long> createComment(
            @Valid @RequestBody CommentCreateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long commentId = commentService.createComment(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentId);
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        commentService.updateComment(commentId, request, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 단건 삭제", description = "댓글을 soft delete합니다.")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.deleteComment(commentId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}

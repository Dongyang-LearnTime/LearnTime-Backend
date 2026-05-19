package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.service.core.PostService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.service.facade.CommunityFacade;
import jakarta.servlet.http.HttpServletRequest;
import learntime.backend.global.utils.IpUtil;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;

import learntime.backend.domain.community.dto.request.PostUpdateRequestDTO;
import learntime.backend.domain.community.dto.response.PostUpdateDetailDTO;
import learntime.backend.global.dto.PageResponse;

@RestController
@RequestMapping("/api/community/post")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 게시글 API", description = "커뮤니티 게시글 CRUD 입니다. (조회 제외 JWT 필요)")
public class PostController {

    private final PostService postService;
    private final CommunityFacade communityFacade;

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "오프셋 기반 페이징으로 게시글 목록을 조회합니다.")
    public ResponseEntity<PageResponse<PostListResponseDTO>> getPostList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostListResponseDTO> response = postService.getPostList(pageable);
        return ResponseEntity.ok(PageResponse.of(response));
    }

    @GetMapping("/{postId}")
    @Operation(
            summary = "게시글 단건 조회",
            description = "게시글의 상세 정보, 이미지, 댓글 및 연관된 공부 핵심 지표를 반환합니다. JWT가 있다면 좋아요 여부도 반환합니다.")
    public ResponseEntity<PostResponseDTO> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long lastCommentId,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = userDetails != null ? userDetails.userId() : null;
        String clientIp = IpUtil.getClientIp(request);
        PostResponseDTO response = communityFacade.getPostDetails(postId, userId, clientIp, lastCommentId, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/edit")
    @Operation(summary = "게시글 수정용 상세 정보 조회", description = "게시글 수정을 위해 필요한 정보(제목, 내용, 연결된 스터디, 기존 이미지)만 조회합니다.")
    public ResponseEntity<PostUpdateDetailDTO> getPostForUpdate(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostUpdateDetailDTO response = postService.getPostForUpdate(postId, userDetails.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다. 이미지는 S3에 저장하며, 최대 3개까지 첨부 가능합니다.")
    public ResponseEntity<Long> createPost(
            @Valid @RequestPart(value = "request") PostCreateRequestDTO request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long postId = postService.createPost(request, images, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다. 기존 이미지 삭제 및 새로운 이미지 추가가 가능합니다.")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @Valid @RequestPart(value = "request") PostUpdateRequestDTO request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        postService.updatePost(postId, request, newImages, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 단건 삭제", description = "게시글과 하위 테이블을 soft delete합니다.")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}

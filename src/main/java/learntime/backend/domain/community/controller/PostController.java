package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.service.PostService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/community/post")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 게시글 API", description = "커뮤니티 게시글 CRUD 입니다. (조회 제외 JWT 필요)")
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다. 이미지는 최대 3개까지 첨부 가능합니다.")
    public ResponseEntity<Long> createPost(
            @Valid @RequestPart(value = "request") PostCreateRequestDTO request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long postId = postService.createPost(request, images, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

}

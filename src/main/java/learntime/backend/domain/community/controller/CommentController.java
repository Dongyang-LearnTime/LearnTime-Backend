package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/comment")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 댓글 API", description = "커뮤니티 댓글 CRUD 입니다. (조회 제외 JWT 필요")
public class CommentController {
}

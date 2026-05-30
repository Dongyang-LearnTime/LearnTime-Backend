package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.community.dto.response.PointRankingResponseDTO;
import learntime.backend.domain.community.service.CommunityService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import learntime.backend.global.dto.PageResponse;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 API", description = "커뮤니티의 랭킹 순위 등을 관리합니다.")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "랭킹", description = "포인트 수가 많은 내림차순으로 순위 정렬")
    @GetMapping("/ranking")
    public ResponseEntity<PageResponse<PointRankingResponseDTO>> getRanking(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        Page<PointRankingResponseDTO> result =
                communityService.getPointRanking(pageable, userId);

        return ResponseEntity.ok(PageResponse.of(result));
    }

}

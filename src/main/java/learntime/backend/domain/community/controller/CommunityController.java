package learntime.backend.domain.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.community.dto.response.PointRankingResponseDTO;
import learntime.backend.domain.community.service.core.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 API", description = "커뮤니티의 랭킹 순위 등을 관리합니다.")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "랭킹", description = "포인트 수가 많은 내림차순으로 순위 정렬")
    @GetMapping("/ranking")
    public ResponseEntity<Page<PointRankingResponseDTO>> getRanking(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(communityService.getPointRanking(pageable));
    }

}

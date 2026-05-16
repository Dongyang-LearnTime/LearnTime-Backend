package learntime.backend.domain.point.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.point.dto.PointRankingResponseDTO;
import learntime.backend.domain.point.service.PointService;
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
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Tag(name = "포인트 조회 API", description = "전체 사용자의 소지 포인트를 조회해 내림차순으로 랭킹 선정")
public class PointController {
    private final PointService pointService;

    @Operation(summary = "랭킹", description = "포인트 수가 많은 내림차순으로 순위 정렬")
    @GetMapping("/ranking")
    public ResponseEntity<Page<PointRankingResponseDTO>> getRanking(
            // 동일 포인트일 경우, 유저 ID로 순서정렬
            @PageableDefault(sort = {"point", "userId"}, direction = Sort.Direction.DESC)Pageable pageable) {
        return ResponseEntity.ok(pointService.getPointRanking(pageable));
    }
}

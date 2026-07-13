package learntime.backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.admin.dto.response.SiteStatsResponseDTO;
import learntime.backend.domain.admin.service.AdminSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site")
@RequiredArgsConstructor
@Tag(name = "관리자 사이트 현황 API", description = "관리자 권한으로 사이트 전체 현황을 조회하는 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;

    @GetMapping("/stats")
    @Operation(summary = "사이트 현황 통계 조회", description = "가입자, 게시글, 댓글의 누적 및 금일 발생 건수를 조회합니다.")
    public ResponseEntity<SiteStatsResponseDTO> getSiteStats() {
        SiteStatsResponseDTO stats = adminSiteService.getSiteStats();
        return ResponseEntity.ok(stats);
    }
}

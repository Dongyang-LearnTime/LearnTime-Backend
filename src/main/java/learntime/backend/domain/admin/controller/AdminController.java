package learntime.backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.admin.service.SiteTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자 API", description = "사이트 기능 및 사용자 관리를 담당하는 관리자 API (JWT 및 ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SiteTestService siteTestService;

    @PostMapping("/test/email/{userId}")
    @Operation(summary = "이메일 전송 테스트", description = "사용자의 userId를 받아 이메일 전송을 테스트함.")
    public ResponseEntity<Void> testSendEmail(@PathVariable Long userId) {
        siteTestService.testSendEmail(userId);
        return ResponseEntity.ok().build();
    }


}

package learntime.backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.admin.dto.request.AdminUserEmailRequest;
import learntime.backend.domain.admin.dto.response.AdminUserDetailResponseDTO;
import learntime.backend.domain.admin.dto.response.AdminUserListResponseDTO;
import learntime.backend.domain.admin.service.AdminUserService;
import learntime.backend.domain.user.enums.Role;
import lombok.RequiredArgsConstructor;
import learntime.backend.global.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "관리자 사용자 관리 API", description = "관리자 권한으로 사용자 조회 및 관리를 담당하는 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "사용자 목록 페이징 조회", description = "전체 사용자를 페이징하여 조회합니다. 이름/이메일 검색 및 권한 필터링이 가능합니다.")
    public ResponseEntity<PageResponse<AdminUserListResponseDTO>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable) {
        
        PageResponse<AdminUserListResponseDTO> result = adminUserService.searchUsers(keyword, role, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 상세 정보 조회", description = "특정 사용자의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponseDTO> getUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponseDTO detail = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/{userId}/role/admin")
    @Operation(summary = "사용자 관리자 권한 부여", description = "특정 일반 사용자를 관리자 권한으로 승격합니다.")
    public ResponseEntity<Void> grantAdminRole(@PathVariable Long userId) {
        adminUserService.grantAdminRole(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 강제 탈퇴", description = "특정 사용자를 강제 탈퇴(소프트 딜리트) 처리합니다.")
    public ResponseEntity<Void> forceWithdrawUser(@PathVariable Long userId) {
        adminUserService.forceWithdrawUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/email")
    @Operation(summary = "사용자 이메일 발송", description = "특정 사용자에게 텍스트 이메일을 발송합니다.")
    public ResponseEntity<Void> sendEmailToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserEmailRequest request) {
        
        adminUserService.sendEmailToUser(userId, request);
        return ResponseEntity.ok().build();
    }
}

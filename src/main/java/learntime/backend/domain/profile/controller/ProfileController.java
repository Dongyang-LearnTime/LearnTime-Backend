package learntime.backend.domain.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.profile.dto.request.ProfileUpdateRequestDTO;
import learntime.backend.domain.profile.dto.response.ProfileResponseDTO;
import learntime.backend.domain.profile.service.ProfileService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile API", description = "프로필 관련. (조회 제외 JWT 필요)")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "프로필 조회", description = "사용자의 프로필 및 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponseDTO> getProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        ProfileResponseDTO response = profileService.getProfile(userId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 프로필 수정", description = "내 프로필 정보(이미지, 설명, 공개여부)를 선택적으로 수정합니다. 이미지 파일 업로드 가능.")
    @PatchMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProfile(
            @RequestPart(value = "request") ProfileUpdateRequestDTO request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        profileService.updateProfile(userDetails.getUserId(), request, image);
        return ResponseEntity.ok().build();
    }

}

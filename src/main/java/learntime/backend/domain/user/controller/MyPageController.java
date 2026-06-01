package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.UpdateNameRequestDTO;
import learntime.backend.domain.user.dto.request.UpdatePasswordRequestDTO;
import learntime.backend.domain.relationship.dto.response.MyBlockedUserListResponseDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.service.MyPageService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.domain.user.dto.request.DeleteAccountRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import learntime.backend.domain.community.dto.response.MyCommentListResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.user.dto.response.MyPageSummaryResponseDTO;
import learntime.backend.domain.user.dto.response.TokenResponseDTO;
import learntime.backend.domain.user.service.AuthService;
import learntime.backend.global.dto.PageResponse;
import learntime.backend.global.utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
@Tag(name = "마이페이지 API", description = "사용자 정보 조회 및 수정 관련 API")
public class MyPageController {

    private final MyPageService myPageService;
    private final AuthService authService;
    private final UserService userService;

    @Value("${jwt.refresh-expiration}")
    private long refreshTime; // 리프레쉬 토큰 유효기간

    @GetMapping
    @Operation(summary = "마이페이지 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<MyPageResponseDTO> getMyPage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MyPageResponseDTO result = myPageService.getMyInfo(userDetails.getUserId());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/name")
    @Operation(summary = "이름 수정", description = "사용자의 이름을 수정하고, 새로운 JWT 토큰을 쿠키와 반환값으로 줍니다.")
    public ResponseEntity<TokenResponseDTO> updateName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid @RequestBody UpdateNameRequestDTO request) {
        AuthService.TokenPair token = myPageService.updateName(userDetails.getUserId(), request.name());

        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie(token.refreshToken(), refreshTime);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponseDTO(token.accessToken(), "Bearer"));
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 수정", description = "사용자의 비밀번호를 수정하고 로그아웃 처리합니다.")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                               HttpServletResponse response,
                                               @Valid @RequestBody UpdatePasswordRequestDTO request) {
        myPageService.updatePassword(userDetails.getUserId(), request.currentPassword(), request.newPassword());
        
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = CookieUtil.createEmptyRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "회원 탈퇴", description = "'회원 탈퇴' 문구를 검증한 후 사용자의 정보를 Soft Delete 및 로그아웃 처리합니다.")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                              HttpServletResponse response,
                                              @Valid @RequestBody DeleteAccountRequestDTO request) {
        
        userService.deleteUser(userDetails.getUserId());

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = CookieUtil.createEmptyRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @Operation(summary = "마이페이지 요약 통계 조회", description = "작성 게시글 수, 댓글 수, 받은 좋아요 합계, 보유 포인트를 반환합니다.")
    public ResponseEntity<MyPageSummaryResponseDTO> getMySummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MyPageSummaryResponseDTO result =
                myPageService.getMySummary(userDetails.getUserId());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/posts")
    @Operation(summary = "내가 쓴 게시글 목록 조회", description = "오프셋 기반 페이징으로 내가 작성한 게시글 목록을 반환합니다.")
    public ResponseEntity<PageResponse<PostListResponseDTO>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PostListResponseDTO> result =
                myPageService.getMyPosts(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/comments")
    @Operation(summary = "내가 쓴 댓글 목록 조회", description = "오프셋 기반 페이징으로 내가 작성한 댓글 목록을 반환합니다.")
    public ResponseEntity<PageResponse<MyCommentListResponseDTO>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<MyCommentListResponseDTO> result =
                myPageService.getMyComments(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/blocks")
    @Operation(summary = "내가 차단한 사용자 목록", description = "오프셋 기반 페이징으로 내가 작성한 댓글 목록을 반환합니다.")
    public ResponseEntity<PageResponse<MyBlockedUserListResponseDTO>> getMyBlockedUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<MyBlockedUserListResponseDTO> result =
                myPageService.getMyBlockedUsers(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(result);
    }


}

package learntime.backend.domain.admin.service;

import learntime.backend.domain.admin.converter.AdminConverter;
import learntime.backend.domain.admin.dto.request.AdminUserEmailRequest;
import learntime.backend.domain.admin.dto.response.AdminUserDetailResponseDTO;
import learntime.backend.domain.admin.dto.response.AdminUserListResponseDTO;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.global.dto.PageResponse;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailUtil emailUtil;

    // 사용자 목록 페이징 조회
    public PageResponse<AdminUserListResponseDTO> searchUsers(String keyword, Role role, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(keyword, role, pageable);
        return PageResponse.of(users.map(AdminConverter::toAdminUserListResponseDTO));
    }

    // 단일 사용자 상세 정보 조회
    public AdminUserDetailResponseDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        
        PromptQuotas quotas = user.getPromptQuotas();
        return AdminConverter.toAdminUserDetailResponseDTO(user, quotas);
    }

    // 사용자 관리자 권한 부여 처리
    @Transactional
    public void grantAdminRole(Long userId) {
        userRepository.updateUserRole(userId, Role.ROLE_ADMIN);
    }

    // 사용자 강제 탈퇴 처리
    @Transactional
    public void forceWithdrawUser(Long userId) {
        // 회원 탈퇴는 기존 UserService 로직 활용 (데이터 정리, soft delete, 익명화 등)
        userService.deleteUser(userId);
    }

    // 사용자 이메일 발송 처리
    public void sendEmailToUser(Long userId, AdminUserEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        String htmlTemplate = loadAdminEmailTemplate();
        htmlTemplate = htmlTemplate.replace("{{TITLE}}", request.subject());
        htmlTemplate = htmlTemplate.replace("{{CONTENT}}", request.content());

        emailUtil.sendHtmlEmail(user.getEmail(), request.subject(), htmlTemplate);
    }

    // 관리자 이메일 템플릿 로드
    private String loadAdminEmailTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/admin-email.html");
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("관리자 이메일 템플릿 로드 실패: {}", e.getMessage());
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }
}

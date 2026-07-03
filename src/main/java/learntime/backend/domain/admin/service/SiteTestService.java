package learntime.backend.domain.admin.service;

import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteTestService {

    private final UserRepository userRepository;
    private final EmailUtil emailUtil;

    public void testSendEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        
        String userEmail = user.getEmail();
        String htmlTemplate = loadSignupAuthHtmlTemplate();
        
        // 테스트용 임시 인증 코드
        String authCode = "123456";
        htmlTemplate = htmlTemplate.replace("{{AUTH_CODE}}", authCode);

        emailUtil.sendHtmlEmail(userEmail, "[Learn-Time] 회원가입 이메일 인증", htmlTemplate);
    }

    private String loadSignupAuthHtmlTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/signup-auth.html");
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("이메일 템플릿 로드 실패: {}", e.getMessage());
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }
}

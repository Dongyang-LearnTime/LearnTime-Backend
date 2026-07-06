package learntime.backend.domain.user.event;

import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private static final String SIGNUP_AUTH_TEMPLATE_PATH = "templates/email/signup-auth.html";
    private static final String SIGNUP_AUTH_SUBJECT = "[Learn-Time] 회원가입 이메일 인증";

    private final EmailUtil emailUtil;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailSendEvent(EmailSendEvent event) {
        try {
            String htmlTemplate = loadSignupAuthHtmlTemplate().replace("{{AUTH_CODE}}", event.authCode());
            emailUtil.sendHtmlEmail(event.email(), SIGNUP_AUTH_SUBJECT, htmlTemplate);
        } catch (Exception e) {
            log.error("비동기 이메일 발송 실패: {}", e.getMessage(), e);
        }
    }

    private String loadSignupAuthHtmlTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(SIGNUP_AUTH_TEMPLATE_PATH);
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("이메일 템플릿 로드 실패: {}", e.getMessage());
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }
}

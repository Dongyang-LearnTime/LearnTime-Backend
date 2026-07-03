package learntime.backend.domain.admin.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteTestService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public void testSendEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        try {
            String userEmail = user.getEmail();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("cka8701@gmail.com");
            helper.setTo(userEmail);
            helper.setSubject("learn-time 테스트 이메일 전송");
            helper.setText("learn-time 테스트 이메일 전송");

            mailSender.send(message);
            log.info("{} 에게 이메일 테스트 전송 성공", userEmail);
        } catch (MessagingException e) {
            log.error(e.getMessage());
        }
    }

}

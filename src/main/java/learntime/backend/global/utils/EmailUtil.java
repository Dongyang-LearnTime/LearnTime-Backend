package learntime.backend.global.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import learntime.backend.global.error.code.EmailErrorCode;
import learntime.backend.global.error.exception.EmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender mailSender;

    public void sendHtmlEmail(String to, String subject, String htmlTemplate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // 인라인 이미지를 위해 MULTIPART_MODE_MIXED_RELATED(true) 사용 (이메일 클라이언트 호환성 향상)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);

            // 명시적으로 image/png Content-Type을 지정하고 cid에 확장자를 포함하여 호환성 향상
            helper.addInline("site-logo.png", new ClassPathResource("static/images/site-logo.png"), "image/png");

            mailSender.send(message);
            log.info("{} 에게 이메일 전송 성공", to);
        } catch (MessagingException e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new EmailException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(message);
            log.info("{} 에게 텍스트 이메일 전송 성공", to);
        } catch (MessagingException e) {
            log.error("텍스트 이메일 전송 실패: {}", e.getMessage(), e);
            throw new EmailException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }
}

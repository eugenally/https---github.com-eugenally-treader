package com.edu.bootstring.member;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 회원 관련 메일 발송. 개발 환경에서는 MailHog(localhost:1025)가 받아
 * <a href="http://localhost:8025">웹 UI</a> 에서 확인할 수 있다.
 *
 * <p>발송은 비동기다. SMTP 가 느리거나 죽어 있다고 해서 가입 트랜잭션이 같이 실패하면 안 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@treader.local}")
    private String from;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Async
    public void sendSignupVerification(String to, String name, String token) {
        String link = "%s/verify-email?token=%s".formatted(frontendBaseUrl, token);
        String html = """
                <div style="font-family:sans-serif;max-width:520px">
                  <h2>TreaderAPP 이메일 인증</h2>
                  <p><strong>%s</strong>님, 가입해 주셔서 감사합니다.</p>
                  <p>아래 버튼을 눌러 이메일 인증을 완료하면 로그인할 수 있습니다.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#1976d2;color:#fff;padding:12px 20px;
                       border-radius:4px;text-decoration:none">이메일 인증하기</a>
                  </p>
                  <p style="color:#666;font-size:13px">
                    버튼이 눌리지 않으면 아래 주소를 복사해 주소창에 붙여 넣으세요.<br/>%s
                  </p>
                  <p style="color:#666;font-size:13px">이 링크는 %d시간 동안 유효합니다.</p>
                </div>
                """.formatted(escape(name), link, link, EmailToken.SIGNUP_VALID_HOURS);

        send(to, "[TreaderAPP] 이메일 인증을 완료해 주세요", html);
    }

    @Async
    public void sendTemporaryPassword(String to, String name, String temporaryPassword) {
        String html = """
                <div style="font-family:sans-serif;max-width:520px">
                  <h2>TreaderAPP 임시 비밀번호</h2>
                  <p><strong>%s</strong>님, 요청하신 임시 비밀번호입니다.</p>
                  <p style="font-size:20px;letter-spacing:2px;background:#f5f5f5;
                     padding:14px 18px;border-radius:4px"><strong>%s</strong></p>
                  <p style="color:#c00">로그인 후 반드시 비밀번호를 변경해 주세요.</p>
                </div>
                """.formatted(escape(name), escape(temporaryPassword));

        send(to, "[TreaderAPP] 임시 비밀번호 안내", html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("메일 발송 완료 to={} subject={}", to, subject);
        } catch (MailException | jakarta.mail.MessagingException e) {
            // 메일 실패로 가입 자체를 막지는 않는다. 사용자는 '인증메일 재발송'으로 복구할 수 있다.
            log.error("메일 발송 실패 to={} subject={} : {}", to, subject, e.getMessage());
        }
    }

    private String escape(String raw) {
        return raw == null ? "" : raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

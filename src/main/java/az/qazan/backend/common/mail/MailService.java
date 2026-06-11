package az.qazan.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Spring Mail. SMTP is optional: when no
 * {@code spring.mail.host} is configured there is no {@link JavaMailSender}
 * bean, so we fall back to logging the message. That keeps local/dev and
 * unconfigured environments fully functional — the password-reset flow
 * still works, you just read the code from the logs instead of an inbox.
 */
@Service
@Slf4j
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${qazan.mail.from:no-reply@qazan.az}")
    private String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetCode(String email, String code) {
        send(email, "Qazan — şifrə sıfırlama kodu",
                "Şifrə sıfırlama kodun: " + code + "\n\n"
                        + "Kod 15 dəqiqə ərzində etibarlıdır. Bu sorğunu sən "
                        + "etməmisənsə, bu məktubu nəzərə alma.");
    }

    private void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("[mail] SMTP not configured — '{}' to {}:\n{}",
                    subject, to, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom(from);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (Exception e) {
            log.warn("[mail] failed to send '{}' to {}: {}",
                    subject, to, e.getMessage());
        }
    }
}

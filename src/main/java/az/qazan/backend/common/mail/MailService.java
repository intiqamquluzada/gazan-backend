package az.qazan.backend.common.mail;

import az.qazan.backend.user.domain.AppLocale;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/**
 * Transactional email. Messages go out as branded HTML (green Qazan card,
 * logo embedded inline so it shows even when remote images are blocked)
 * with a plain-text alternative for clients that prefer it.
 *
 * <p>SMTP is optional: when no {@code spring.mail.host} is configured there
 * is no {@link JavaMailSender} bean, so we fall back to logging. That keeps
 * local/dev and unconfigured environments fully functional.
 */
@Service
@Slf4j
public class MailService {

    private static final String LOGO_CID = "qazan-logo";
    private static final ClassPathResource LOGO = new ClassPathResource("mail/logo.png");

    private final ObjectProvider<JavaMailSender> mailSender;

    private final Environment environment;

    @Value("${qazan.mail.from:no-reply@qazan.az}")
    private String from;

    @Value("${spring.mail.host:}")
    private String host;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    /** The reset code, written in the account's own language. */
    public void sendPasswordResetCode(String email, String code, AppLocale locale) {
        ResetTexts t = ResetTexts.of(locale == null ? AppLocale.AZ : locale);
        String text = t.intro + "\n\n" + code + "\n\n" + t.validity + "\n" + t.ignore
                + "\n\n— Qazan";
        send(email, t.subject, text, resetHtml(t, code));
    }

    // ───────────────────────────── template ─────────────────────────────

    private record ResetTexts(String lang, String subject, String heading, String intro,
                              String codeLabel, String validity, String ignore,
                              String tagline) {
        static ResetTexts of(AppLocale l) {
            return switch (l) {
                case TR -> new ResetTexts("tr", "Qazan — şifre sıfırlama kodu",
                        "Şifreni sıfırla",
                        "Qazan hesabın için bir şifre sıfırlama isteği aldık. "
                                + "Uygulamaya aşağıdaki kodu gir:",
                        "Doğrulama kodu",
                        "Kod 15 dakika geçerlidir.",
                        "Bu isteği sen yapmadıysan bu e-postayı dikkate alma; "
                                + "şifren değişmeyecek.",
                        "Sevdiğin yerlerden kazan");
                case EN -> new ResetTexts("en", "Qazan — password reset code",
                        "Reset your password",
                        "We received a request to reset the password of your "
                                + "Qazan account. Enter this code in the app:",
                        "Verification code",
                        "The code is valid for 15 minutes.",
                        "If you didn't ask for this, you can ignore this email — "
                                + "your password won't change.",
                        "Earn at the places you love");
                case RU -> new ResetTexts("ru", "Qazan — код для сброса пароля",
                        "Сброс пароля",
                        "Мы получили запрос на сброс пароля вашего аккаунта "
                                + "Qazan. Введите этот код в приложении:",
                        "Код подтверждения",
                        "Код действует 15 минут.",
                        "Если вы не запрашивали сброс, просто проигнорируйте "
                                + "это письмо — пароль не изменится.",
                        "Зарабатывайте в любимых местах");
                default -> new ResetTexts("az", "Qazan — şifrə sıfırlama kodu",
                        "Şifrəni sıfırla",
                        "Qazan hesabın üçün şifrə sıfırlama sorğusu aldıq. "
                                + "Tətbiqə aşağıdakı kodu daxil et:",
                        "Təsdiq kodu",
                        "Kod 15 dəqiqə ərzində etibarlıdır.",
                        "Bu sorğunu sən etməmisənsə, bu məktubu nəzərə alma — "
                                + "şifrən dəyişməyəcək.",
                        "Sevdiyin yerlərdən qazan");
            };
        }
    }

    private static String resetHtml(ResetTexts t, String code) {
        String c = HtmlUtils.htmlEscape(code);
        return """
            <!DOCTYPE html>
            <html lang="%s"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="light only">
            <title>%s</title></head>
            <body style="margin:0;padding:0;background:#F5FAF3;">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#F5FAF3;">
             <tr><td align="center" style="padding:32px 16px;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:480px;">
               <tr><td align="center" style="padding:0 0 20px;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0"><tr>
                 <td style="vertical-align:middle;"><img src="cid:%s" width="44" height="44" alt="Qazan" style="display:block;border:0;border-radius:12px;"></td>
                 <td style="vertical-align:middle;padding-left:10px;font-family:Segoe UI,Helvetica,Arial,sans-serif;font-size:24px;font-weight:800;color:#1C4A2A;letter-spacing:-0.3px;">Qazan</td>
                </tr></table>
               </td></tr>
               <tr><td style="background:#FFFFFF;border-radius:20px;box-shadow:0 8px 24px rgba(30,58,36,0.08);overflow:hidden;">
                <div style="height:6px;background:#2A8236;background-image:linear-gradient(90deg,#55AD45,#2A8236);border-radius:20px 20px 0 0;"></div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                 <tr><td style="padding:28px 28px 8px;font-family:Segoe UI,Helvetica,Arial,sans-serif;">
                  <h1 style="margin:0 0 10px;font-size:22px;line-height:1.3;color:#1C4A2A;font-weight:700;">%s</h1>
                  <p style="margin:0;font-size:15px;line-height:1.55;color:#5F6B63;">%s</p>
                 </td></tr>
                 <tr><td style="padding:20px 28px;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#E8F5E3;border-radius:16px;">
                   <tr><td align="center" style="padding:18px 12px 6px;font-family:Segoe UI,Helvetica,Arial,sans-serif;font-size:12px;font-weight:600;letter-spacing:1px;text-transform:uppercase;color:#23702E;">%s</td></tr>
                   <tr><td align="center" style="padding:0 12px 18px;font-family:Consolas,Menlo,Courier New,monospace;font-size:34px;font-weight:700;letter-spacing:10px;color:#1C4A2A;">%s</td></tr>
                  </table>
                 </td></tr>
                 <tr><td style="padding:0 28px 28px;font-family:Segoe UI,Helvetica,Arial,sans-serif;">
                  <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#1A2B20;font-weight:600;">&#9201; %s</p>
                  <p style="margin:0;font-size:13px;line-height:1.55;color:#828C85;">%s</p>
                 </td></tr>
                </table>
               </td></tr>
               <tr><td align="center" style="padding:20px 8px 0;font-family:Segoe UI,Helvetica,Arial,sans-serif;font-size:12px;line-height:1.6;color:#828C85;">
                %s<br>&copy; Qazan &middot; <a href="https://api.qazan.app/legal/privacy?lang=%s" style="color:#23702E;text-decoration:none;">qazan.app</a>
               </td></tr>
              </table>
             </td></tr>
            </table>
            </body></html>
            """.formatted(
                t.lang, HtmlUtils.htmlEscape(t.subject), LOGO_CID,
                HtmlUtils.htmlEscape(t.heading), HtmlUtils.htmlEscape(t.intro),
                HtmlUtils.htmlEscape(t.codeLabel), c,
                HtmlUtils.htmlEscape(t.validity), HtmlUtils.htmlEscape(t.ignore),
                HtmlUtils.htmlEscape(t.tagline), t.lang);
    }

    // ───────────────────────────── transport ────────────────────────────

    private void send(String to, String subject, String text, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || host == null || host.isBlank()) {
            // The body can hold a live reset code, so it stays out of the
            // logs everywhere except a developer's own machine.
            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                log.warn("[mail] SMTP not configured — '{}' to {}:\n{}",
                        subject, to, text);
            } else {
                log.warn("[mail] SMTP not configured — dropped '{}'", subject);
            }
            return;
        }
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(
                    msg, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            h.setTo(to);
            h.setFrom(from);
            h.setSubject(subject);
            h.setText(text, html);
            if (LOGO.exists()) {
                h.addInline(LOGO_CID, LOGO, "image/png");
            }
            sender.send(msg);
            log.info("[mail] sent '{}'", subject);
        } catch (Exception e) {
            log.warn("[mail] failed to send '{}': {}", subject, e.getMessage());
        }
    }
}

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
 * Transactional email. Messages go out as branded HTML (orange Qazan card,
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
        sendCode(email, code, CodeTexts.reset(locale == null ? AppLocale.AZ : locale));
    }

    /** Sign-up code: proves the address before the account is created. */
    public void sendEmailVerificationCode(String email, String code, AppLocale locale) {
        sendCode(email, code, CodeTexts.verify(locale == null ? AppLocale.AZ : locale));
    }

    private void sendCode(String email, String code, CodeTexts t) {
        String text = t.heading + "\n\n" + t.intro + "\n\n    " + code + "\n\n"
                + t.validity + "\n" + t.security + "\n" + t.ignore + "\n\n— Qazan";
        send(email, t.subject, text, codeHtml(t, code));
    }

    // ───────────────────────────── texts ────────────────────────────────

    /** Every string of a code email in one language. */
    record CodeTexts(String lang, String subject, String preheader, String heading,
                     String intro, String codeLabel, String validity, String security,
                     String ignore, String tagline) {

        static CodeTexts verify(AppLocale l) {
            return switch (l) {
                case TR -> new CodeTexts("tr", "Qazan — e-posta doğrulama kodun",
                        "Doğrulama kodun:",
                        "E-postanı doğrula",
                        "Qazan'a hoş geldin! Kaydını tamamlamak için bu kodu "
                                + "uygulamaya gir:",
                        "Doğrulama kodu",
                        "Kod 15 dakika geçerlidir",
                        "Bu kodu kimseyle paylaşma — Qazan ekibi onu asla sormaz.",
                        "Kaydı sen başlatmadıysan bu e-postayı dikkate alma; "
                                + "hesap oluşturulmayacak.",
                        "Sevdiğin yerlerden kazan");
                case EN -> new CodeTexts("en", "Qazan — your email verification code",
                        "Your verification code:",
                        "Verify your email",
                        "Welcome to Qazan! Enter this code in the app to finish "
                                + "creating your account:",
                        "Verification code",
                        "The code is valid for 15 minutes",
                        "Never share this code — the Qazan team will never ask for it.",
                        "If you didn't start signing up, just ignore this email — "
                                + "no account will be created.",
                        "Earn at the places you love");
                case RU -> new CodeTexts("ru", "Qazan — код подтверждения почты",
                        "Ваш код подтверждения:",
                        "Подтвердите почту",
                        "Добро пожаловать в Qazan! Введите этот код в приложении, "
                                + "чтобы завершить регистрацию:",
                        "Код подтверждения",
                        "Код действует 15 минут",
                        "Никому не сообщайте этот код — команда Qazan никогда его "
                                + "не спрашивает.",
                        "Если вы не начинали регистрацию, просто проигнорируйте "
                                + "это письмо — аккаунт не будет создан.",
                        "Зарабатывайте в любимых местах");
                default -> new CodeTexts("az", "Qazan — e-poçt təsdiq kodun",
                        "Təsdiq kodun:",
                        "E-poçtunu təsdiqlə",
                        "Qazan-a xoş gəldin! Qeydiyyatı tamamlamaq üçün bu kodu "
                                + "tətbiqə daxil et:",
                        "Təsdiq kodu",
                        "Kod 15 dəqiqə ərzində etibarlıdır",
                        "Bu kodu heç kimlə paylaşma — Qazan komandası onu heç vaxt "
                                + "soruşmur.",
                        "Qeydiyyatı sən başlatmamısansa, bu məktubu nəzərə alma — "
                                + "hesab yaradılmayacaq.",
                        "Sevdiyin yerlərdən qazan");
            };
        }

        static CodeTexts reset(AppLocale l) {
            return switch (l) {
                case TR -> new CodeTexts("tr", "Qazan — şifre sıfırlama kodu",
                        "Şifre sıfırlama kodun:",
                        "Şifreni sıfırla",
                        "Qazan hesabın için bir şifre sıfırlama isteği aldık. "
                                + "Uygulamaya aşağıdaki kodu gir:",
                        "Doğrulama kodu",
                        "Kod 15 dakika geçerlidir",
                        "Bu kodu kimseyle paylaşma — Qazan ekibi onu asla sormaz.",
                        "Bu isteği sen yapmadıysan bu e-postayı dikkate alma; "
                                + "şifren değişmeyecek.",
                        "Sevdiğin yerlerden kazan");
                case EN -> new CodeTexts("en", "Qazan — password reset code",
                        "Your password reset code:",
                        "Reset your password",
                        "We received a request to reset the password of your "
                                + "Qazan account. Enter this code in the app:",
                        "Verification code",
                        "The code is valid for 15 minutes",
                        "Never share this code — the Qazan team will never ask for it.",
                        "If you didn't ask for this, you can ignore this email — "
                                + "your password won't change.",
                        "Earn at the places you love");
                case RU -> new CodeTexts("ru", "Qazan — код для сброса пароля",
                        "Код для сброса пароля:",
                        "Сброс пароля",
                        "Мы получили запрос на сброс пароля вашего аккаунта "
                                + "Qazan. Введите этот код в приложении:",
                        "Код подтверждения",
                        "Код действует 15 минут",
                        "Никому не сообщайте этот код — команда Qazan никогда его "
                                + "не спрашивает.",
                        "Если вы не запрашивали сброс, просто проигнорируйте "
                                + "это письмо — пароль не изменится.",
                        "Зарабатывайте в любимых местах");
                default -> new CodeTexts("az", "Qazan — şifrə sıfırlama kodu",
                        "Şifrə sıfırlama kodun:",
                        "Şifrəni sıfırla",
                        "Qazan hesabın üçün şifrə sıfırlama sorğusu aldıq. "
                                + "Tətbiqə aşağıdakı kodu daxil et:",
                        "Təsdiq kodu",
                        "Kod 15 dəqiqə ərzində etibarlıdır",
                        "Bu kodu heç kimlə paylaşma — Qazan komandası onu heç vaxt "
                                + "soruşmur.",
                        "Bu sorğunu sən etməmisənsə, bu məktubu nəzərə alma — "
                                + "şifrən dəyişməyəcək.",
                        "Sevdiyin yerlərdən qazan");
            };
        }
    }

    // ───────────────────────────── template ─────────────────────────────

    // App palette (gazan-mobile AppColors): cream canvas, orange gradient,
    // warm brown text. Gradients get a solid bgcolor fallback for Outlook.
    private static final String FONT =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    /** Branded code email: orange hero, logo, the code as six digit tiles. */
    static String codeHtml(CodeTexts t, String code) {
        String safe = HtmlUtils.htmlEscape(code);
        StringBuilder tiles = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            if (i > 0) {
                tiles.append("<td width=\"6\" style=\"width:6px;font-size:0;\">&nbsp;</td>");
            }
            tiles.append("""
                <td align="center" valign="middle" width="42" height="56" style="width:42px;height:56px;background:#FFF1E7;border:1.5px solid #FFD2B0;border-radius:14px;font-family:%s;font-size:28px;font-weight:800;color:#1F1712;line-height:56px;">%s</td>"""
                    .formatted(FONT, HtmlUtils.htmlEscape(String.valueOf(code.charAt(i)))));
        }
        return """
            <!DOCTYPE html>
            <html lang="%1$s"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <title>%2$s</title></head>
            <body style="margin:0;padding:0;background:#FFF8F3;-webkit-text-size-adjust:100%%;">
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:#FFF8F3;">%3$s %4$s</div>
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#FFF8F3" style="background:#FFF8F3;">
             <tr><td align="center" style="padding:36px 14px 28px;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:500px;">

               <tr><td style="background:#FFFFFF;border:1px solid #FFE6D3;border-radius:28px;overflow:hidden;box-shadow:0 14px 40px rgba(74,42,20,0.10);">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">

                 <tr><td align="center" bgcolor="#F06416" style="background:#F06416;background-image:linear-gradient(135deg,#FFA93A 0%%,#F76B15 100%%);border-radius:27px 27px 0 0;padding:34px 24px 30px;">
                  <table role="presentation" cellspacing="0" cellpadding="0" border="0"><tr>
                   <td style="background:#FFFFFF;border-radius:20px;padding:4px;box-shadow:0 6px 18px rgba(120,45,0,0.25);">
                    <img src="cid:%5$s" width="60" height="60" alt="Qazan" style="display:block;border:0;border-radius:16px;">
                   </td>
                  </tr></table>
                  <div style="padding-top:12px;font-family:%6$s;font-size:15px;font-weight:800;letter-spacing:2px;text-transform:uppercase;color:#FFE6D5;">Qazan</div>
                  <h1 style="margin:6px 0 0;font-family:%6$s;font-size:26px;line-height:1.25;font-weight:800;color:#FFFFFF;letter-spacing:-0.3px;">%7$s</h1>
                 </td></tr>

                 <tr><td style="padding:28px 30px 6px;font-family:%6$s;">
                  <p style="margin:0;font-size:15px;line-height:1.6;color:#6E6259;text-align:center;">%8$s</p>
                 </td></tr>

                 <tr><td align="center" style="padding:20px 18px 4px;">
                  <div style="font-family:%6$s;font-size:11px;font-weight:700;letter-spacing:1.6px;text-transform:uppercase;color:#B54A0C;padding-bottom:12px;">%9$s</div>
                  <table role="presentation" cellspacing="0" cellpadding="0" border="0" aria-label="%4$s"><tr>%10$s</tr></table>
                 </td></tr>

                 <tr><td align="center" style="padding:18px 24px 8px;">
                  <table role="presentation" cellspacing="0" cellpadding="0" border="0"><tr>
                   <td style="background:#FFEDE0;border-radius:999px;padding:8px 16px;font-family:%6$s;font-size:13px;font-weight:700;color:#B54A0C;">&#9201;&nbsp; %11$s</td>
                  </tr></table>
                 </td></tr>

                 <tr><td style="padding:22px 30px 0;"><div style="height:1px;background:#FFE6D3;line-height:1px;font-size:0;">&nbsp;</div></td></tr>

                 <tr><td style="padding:18px 30px 28px;font-family:%6$s;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"><tr>
                   <td width="34" valign="top" style="width:34px;font-size:18px;line-height:1.4;">&#128274;</td>
                   <td valign="top" style="font-size:13px;line-height:1.55;color:#1F1712;font-weight:600;">%12$s</td>
                  </tr><tr>
                   <td width="34" valign="top" style="width:34px;padding-top:10px;font-size:18px;line-height:1.4;">&#9993;&#65039;</td>
                   <td valign="top" style="padding-top:10px;font-size:13px;line-height:1.55;color:#6E6259;">%13$s</td>
                  </tr></table>
                 </td></tr>

                </table>
               </td></tr>

               <tr><td align="center" style="padding:22px 10px 0;font-family:%6$s;font-size:12px;line-height:1.7;color:#9A8C82;">
                <span style="color:#F06416;font-weight:700;">%14$s</span><br>
                &copy; Qazan &middot; <a href="https://api.qazan.app/legal/privacy?lang=%1$s" style="color:#B54A0C;text-decoration:none;font-weight:600;">qazan.app</a>
               </td></tr>

              </table>
             </td></tr>
            </table>
            </body></html>
            """.formatted(
                t.lang, HtmlUtils.htmlEscape(t.subject), HtmlUtils.htmlEscape(t.preheader),
                safe, LOGO_CID, FONT, HtmlUtils.htmlEscape(t.heading),
                HtmlUtils.htmlEscape(t.intro), HtmlUtils.htmlEscape(t.codeLabel),
                tiles, HtmlUtils.htmlEscape(t.validity), HtmlUtils.htmlEscape(t.security),
                HtmlUtils.htmlEscape(t.ignore), HtmlUtils.htmlEscape(t.tagline));
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

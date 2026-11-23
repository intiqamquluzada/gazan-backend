package az.qazan.backend.common.mail;

import az.qazan.backend.user.domain.AppLocale;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplateTests {

    @Test
    void codeEmailShowsEveryDigitInEachLanguage() throws Exception {
        Path out = Path.of("build", "mail-preview");
        Files.createDirectories(out);
        for (AppLocale l : AppLocale.values()) {
            for (String kind : new String[] {"verify", "reset"}) {
                MailService.CodeTexts t = kind.equals("verify")
                        ? MailService.CodeTexts.verify(l) : MailService.CodeTexts.reset(l);
                String html = MailService.codeHtml(t, "482913");
                assertThat(html).contains("lang=\"" + t.lang() + "\"")
                        .contains(">4<").contains(">3<")
                        .contains("cid:qazan-logo")
                        .doesNotContain("%s").doesNotContain("%1$");
                // Open build/mail-preview/*.html to eyeball the design.
                Files.writeString(out.resolve(kind + "-" + t.lang() + ".html"), html);
            }
        }
    }
}

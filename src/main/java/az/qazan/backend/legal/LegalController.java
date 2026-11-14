package az.qazan.backend.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Public legal pages linked from the app and the store listings:
 * privacy policy, terms of use and account deletion. Plain server-rendered
 * HTML, no login, no external assets.
 *
 * <p>Az and tr are the complete versions (the app ships in Azerbaijan and
 * Türkiye); en and ru are summaries. Controller identity, contact e-mail,
 * address and effective date come from configuration and are shown as a
 * visible "to be completed" marker until they are set — nothing is invented.
 */
@RestController
@RequestMapping("/legal")
@Tag(name = "Legal")
public class LegalController {

    private static final Set<String> LANGS = Set.of("az", "tr", "en", "ru");

    private final String entity;
    private final String email;
    private final String address;
    private final String date;

    public LegalController(@Value("${qazan.legal.entity:}") String entity,
                           @Value("${qazan.legal.email:}") String email,
                           @Value("${qazan.legal.address:}") String address,
                           @Value("${qazan.legal.effective-date:}") String date) {
        this.entity = entity;
        this.email = email;
        this.address = address;
        this.date = date;
    }

    @Operation(summary = "Privacy policy (HTML)")
    @GetMapping("/privacy")
    public ResponseEntity<String> privacy(@RequestParam(required = false) String lang,
                                          @RequestHeader(value = "Accept-Language", required = false) String al) {
        String l = lang(lang, al);
        return html(l, LegalTexts.privacy(l, ctx(l)));
    }

    @Operation(summary = "Terms of use (HTML)")
    @GetMapping("/terms")
    public ResponseEntity<String> terms(@RequestParam(required = false) String lang,
                                        @RequestHeader(value = "Accept-Language", required = false) String al) {
        String l = lang(lang, al);
        return html(l, LegalTexts.terms(l, ctx(l)));
    }

    @Operation(summary = "How to delete your account (HTML)")
    @GetMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(@RequestParam(required = false) String lang,
                                                @RequestHeader(value = "Accept-Language", required = false) String al) {
        String l = lang(lang, al);
        return html(l, LegalTexts.deletion(l, ctx(l)));
    }

    // ─────────────────────────── helpers ───────────────────────────

    private LegalTexts.Ctx ctx(String l) {
        String todo = switch (l) {
            case "tr" -> "TAMAMLANACAK";
            case "en" -> "TO BE COMPLETED";
            case "ru" -> "БУДЕТ ДОПОЛНЕНО";
            default -> "TAMAMLANACAQ";
        };
        return new LegalTexts.Ctx(
                fill(entity, todo), fill(email, todo), fill(address, todo), fill(date, todo));
    }

    private static String fill(String v, String todo) {
        return v == null || v.isBlank()
                ? "<mark class=\"todo\">[" + todo + "]</mark>"
                : LegalTexts.esc(v.trim());
    }

    static String lang(String param, String acceptLanguage) {
        if (param != null && LANGS.contains(param.toLowerCase(Locale.ROOT))) {
            return param.toLowerCase(Locale.ROOT);
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                for (Locale.LanguageRange r : Locale.LanguageRange.parse(acceptLanguage)) {
                    String tag = r.getRange().toLowerCase(Locale.ROOT);
                    String primary = tag.contains("-") ? tag.substring(0, tag.indexOf('-')) : tag;
                    if (LANGS.contains(primary)) return primary;
                }
            } catch (IllegalArgumentException ignored) {
                // malformed header: fall through to the default
            }
        }
        return "az";
    }

    private static ResponseEntity<String> html(String lang, LegalTexts.Page page) {
        StringBuilder nav = new StringBuilder();
        for (String l : List.of("az", "tr", "en", "ru")) {
            nav.append(l.equals(lang)
                    ? "<b>" + l.toUpperCase(Locale.ROOT) + "</b>"
                    : "<a href=\"?lang=" + l + "\">" + l.toUpperCase(Locale.ROOT) + "</a>");
        }
        String body = """
                <!DOCTYPE html><html lang="%s"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s · Qazan</title><style>
                body{margin:0;background:#F7F4EF;color:#1E2522;font:16px/1.6 -apple-system,Segoe UI,Roboto,Arial,sans-serif}
                main{max-width:760px;margin:0 auto;padding:28px 20px 60px}
                header{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:8px}
                .brand{display:flex;align-items:center;gap:10px;font-weight:800;font-size:20px;color:#2F4A3A}
                .q{width:36px;height:36px;border-radius:11px;background:#2F4A3A;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:900}
                nav{display:flex;gap:10px;font-size:14px} nav a{color:#2F4A3A}
                h1{color:#2F4A3A;font-size:28px;line-height:1.25;margin:22px 0 6px}
                h2{color:#2F4A3A;font-size:19px;margin:28px 0 8px}
                .meta{color:#7C7670;font-size:14px}
                section{background:#fff;border-radius:16px;padding:4px 20px 14px;margin:14px 0;box-shadow:0 1px 2px rgba(0,0,0,.04),0 6px 16px rgba(60,40,20,.05)}
                li{margin:4px 0} a{color:#2F4A3A}
                mark.todo{background:#FBE3DF;color:#A93A2C;padding:0 4px;border-radius:4px;font-weight:700}
                table{border-collapse:collapse;width:100%%;font-size:14px} td,th{border:1px solid #ECE5DB;padding:6px 8px;text-align:left;vertical-align:top}
                </style></head><body><main>
                <header><div class="brand"><span class="q">Q</span>Qazan</div><nav>%s</nav></header>
                <h1>%s</h1><div class="meta">%s</div>
                %s
                </main></body></html>
                """.formatted(lang, page.title(), nav, page.title(), page.meta(), page.body());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }
}

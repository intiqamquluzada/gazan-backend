package az.qazan.backend.user.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Languages the app supports out of the box. Anything else is rejected
 * at the API edge and falls back to {@link #AZ}.
 */
public enum AppLocale {
    AZ, EN, RU, TR;

    public Locale toJavaLocale() {
        return Locale.of(name().toLowerCase(Locale.ROOT));
    }

    public static AppLocale fromTag(String tag) {
        return parse(tag).orElse(AZ);
    }

    public static Optional<AppLocale> parse(String tag) {
        if (tag == null || tag.isBlank()) return Optional.empty();
        String head = tag.split("[-_]", 2)[0].toUpperCase(Locale.ROOT);
        for (AppLocale l : values()) {
            if (l.name().equals(head)) return Optional.of(l);
        }
        return Optional.empty();
    }
}

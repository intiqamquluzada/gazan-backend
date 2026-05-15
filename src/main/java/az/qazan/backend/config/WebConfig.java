package az.qazan.backend.config;

import az.qazan.backend.user.domain.AppLocale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.default-locale:az}")
    private String defaultLocaleTag;

    /**
     * Locale resolution priority:
     * <ol>
     *   <li>{@code Accept-Language} request header (if it maps to a supported tag)</li>
     *   <li>{@code app.default-locale} from configuration</li>
     * </ol>
     *
     * <p>The user's persisted locale (when authenticated) is applied later
     * by an interceptor — but for now this header-based resolver is enough
     * since the mobile app sets the header on every request.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AppLocale fallback = AppLocale.fromTag(defaultLocaleTag);
        return new LocaleResolver() {
            @Override
            @NonNull
            public Locale resolveLocale(@NonNull HttpServletRequest request) {
                String header = request.getHeader("Accept-Language");
                if (header == null || header.isBlank()) return fallback.toJavaLocale();
                // Take the first preference token only — keep it simple.
                String first = header.split(",", 2)[0].trim();
                return AppLocale.parse(first)
                        .map(AppLocale::toJavaLocale)
                        .orElse(fallback.toJavaLocale());
            }

            @Override
            public void setLocale(@NonNull HttpServletRequest request,
                                  HttpServletResponse response,
                                  Locale locale) {
                // Stateless: nothing to persist on the server side.
            }
        };
    }
}

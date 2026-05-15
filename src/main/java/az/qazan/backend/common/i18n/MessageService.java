package az.qazan.backend.common.i18n;

import az.qazan.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Tiny wrapper around Spring's {@link MessageSource} that:
 * <ul>
 *   <li>Defaults to the request's resolved locale</li>
 *   <li>Falls back to the message key itself if a translation is missing
 *       (so we never throw inside an exception handler)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return get(key, LocaleContextHolder.getLocale(), args);
    }

    public String get(String key, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return key;
        }
    }

    public String get(ErrorCode code, Object... args) {
        return get(code.getMessageKey(), args);
    }
}

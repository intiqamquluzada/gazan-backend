package az.qazan.backend.companies.application;

import az.qazan.backend.companies.api.dto.CompanyResponse;
import az.qazan.backend.companies.domain.Company;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Translates a {@link Company} entity into the on-the-wire response,
 * picking the localized {@code name} / {@code tagline} based on the
 * caller's {@code Accept-Language}. When no translation is set for the
 * requested locale we fall back to the Azerbaijani default — so the
 * client never sees an empty string.
 */
@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company c) {
        Locale locale = LocaleContextHolder.getLocale();
        String lang = locale.getLanguage();
        String name = pickLocalized(
                c.getName(), c.getNameEn(), c.getNameRu(), c.getNameTr(), lang);
        String tagline = pickLocalized(
                c.getTagline(), c.getTaglineEn(), c.getTaglineRu(), c.getTaglineTr(), lang);

        return new CompanyResponse(
                c.getId(),
                name,
                tagline,
                c.getCategory(),
                c.getLogoEmoji(),
                c.getCoverColorHex(),
                c.getAddress(),
                c.getRating(),
                c.getReviewCount(),
                c.isFeatured(),
                c.getPhone(),
                c.getInstagram(),
                c.getWorkingHours(),
                c.getLatitude(),
                c.getLongitude(),
                c.getAmenities(),
                c.getPhotoUrls(),
                c.getMenuUrl(),
                c.getCoinRate(),
                c.getLogoUrl(),
                c.getNameEn(),
                c.getNameRu(),
                c.getNameTr(),
                c.getTaglineEn(),
                c.getTaglineRu(),
                c.getTaglineTr()
        );
    }

    /** Picks the field matching {@code lang}; falls back to the AZ default. */
    private static String pickLocalized(String az, String en, String ru, String tr, String lang) {
        String chosen = switch (lang) {
            case "en" -> en;
            case "ru" -> ru;
            case "tr" -> tr;
            default -> null;
        };
        return (chosen != null && !chosen.isBlank()) ? chosen : az;
    }
}

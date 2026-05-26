package az.qazan.backend.companies.api.dto;

import az.qazan.backend.companies.domain.BusinessCategory;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String tagline,
        BusinessCategory category,
        String logoEmoji,
        long coverColorHex,
        String address,
        Double rating,
        int reviewCount,
        boolean featured,
        String phone,
        String instagram,
        String workingHours,
        Double latitude,
        Double longitude,
        String amenities,
        String photoUrls,
        String menuUrl,
        Double coinRate,
        String logoUrl,

        // Raw per-language translations. `name` / `tagline` above are
        // already localized for the caller's Accept-Language; these
        // fields let the business profile editor show what's currently
        // set for every language.
        String nameEn,
        String nameRu,
        String nameTr,
        String taglineEn,
        String taglineRu,
        String taglineTr
) {
}

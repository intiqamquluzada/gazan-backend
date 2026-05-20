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
        String logoUrl
) {
}

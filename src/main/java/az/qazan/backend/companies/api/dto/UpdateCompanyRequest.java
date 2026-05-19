package az.qazan.backend.companies.api.dto;

import az.qazan.backend.companies.domain.BusinessCategory;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(min = 2, max = 120) String name,
        @Size(max = 240) String tagline,
        BusinessCategory category,
        @Size(max = 8) String logoEmoji,
        Long coverColorHex,
        @Size(max = 240) String address,
        @Size(max = 32) String phone,
        @Size(max = 64) String instagram,
        @Size(max = 120) String workingHours,
        Double latitude,
        Double longitude,
        @Size(max = 255) String amenities,
        @Size(max = 2000) String photoUrls,
        @Size(max = 512) String menuUrl,
        Double coinRate
) {
}

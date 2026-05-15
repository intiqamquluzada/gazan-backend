package az.qazan.backend.companies.api.dto;

import az.qazan.backend.companies.domain.BusinessCategory;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(min = 2, max = 120) String name,
        @Size(max = 240) String tagline,
        BusinessCategory category,
        @Size(max = 8) String logoEmoji,
        Long coverColorHex,
        @Size(max = 240) String address
) {
}

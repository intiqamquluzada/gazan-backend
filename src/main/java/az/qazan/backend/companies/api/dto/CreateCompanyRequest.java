package az.qazan.backend.companies.api.dto;

import az.qazan.backend.companies.domain.BusinessCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @Size(max = 240) String tagline,
        @NotNull BusinessCategory category,
        @Size(max = 8) String logoEmoji,
        long coverColorHex,
        @Size(max = 240) String address
) {
}

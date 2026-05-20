package az.qazan.backend.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCompanyFeaturedRequest(@NotNull Boolean featured) {
}

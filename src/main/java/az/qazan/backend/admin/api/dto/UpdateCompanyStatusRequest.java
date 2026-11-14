package az.qazan.backend.admin.api.dto;

import az.qazan.backend.companies.domain.CompanyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCompanyStatusRequest(
        @NotNull CompanyStatus status,
        @Size(max = 300) String reason
) {
}

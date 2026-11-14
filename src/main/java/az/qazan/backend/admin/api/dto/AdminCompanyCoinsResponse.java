package az.qazan.backend.admin.api.dto;

import java.util.UUID;

public record AdminCompanyCoinsResponse(
        UUID companyId,
        String name,
        long earned,
        long spent,
        long grants
) {
}

package az.qazan.backend.coins.api.dto;

import java.util.UUID;

public record CompanyBalanceResponse(
        UUID companyId,
        String companyName,
        String logoUrl,
        long balance
) {
}

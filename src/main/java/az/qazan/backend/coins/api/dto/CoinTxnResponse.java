package az.qazan.backend.coins.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CoinTxnResponse(
        UUID id,
        UUID companyId,
        String companyName,
        int amount,
        String type,
        String note,
        Instant createdAt
) {
}

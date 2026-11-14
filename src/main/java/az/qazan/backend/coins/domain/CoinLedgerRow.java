package az.qazan.backend.coins.domain;

import java.time.Instant;
import java.util.UUID;

public interface CoinLedgerRow {
    int getAmount();
    Instant getCreatedAt();
    UUID getUserId();
}

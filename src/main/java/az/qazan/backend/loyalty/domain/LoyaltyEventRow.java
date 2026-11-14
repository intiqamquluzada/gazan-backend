package az.qazan.backend.loyalty.domain;

import java.time.Instant;
import java.util.UUID;

public interface LoyaltyEventRow {
    LoyaltyEvent.Type getType();
    int getAmount();
    Instant getCreatedAt();
    UUID getUserId();
}

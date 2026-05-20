package az.qazan.backend.business.domain;

import java.time.Instant;
import java.util.UUID;

/** Aggregated view of one customer as seen by a business owner. */
public interface BusinessCustomerProjection {
    UUID getUserId();

    String getFullName();

    String getPhone();

    long getTotalStamps();

    long getRewardsClaimed();

    long getCardCount();

    Instant getLastActivityAt();
}

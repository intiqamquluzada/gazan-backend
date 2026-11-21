package az.qazan.backend.business.domain;

import java.time.Instant;
import java.util.UUID;

/** One customer's coin activity at a venue, as the business sees it. */
public interface CoinCustomerProjection {
    UUID getUserId();

    String getFullName();

    String getPhone();

    /** Coins this customer holds at the venue (earned minus spent). */
    long getBalance();

    Instant getFirstActivityAt();

    Instant getLastActivityAt();
}

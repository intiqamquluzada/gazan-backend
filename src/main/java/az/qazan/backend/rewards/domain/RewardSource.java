package az.qazan.backend.rewards.domain;

/** Where a {@link RewardClaim} came from. */
public enum RewardSource {
    /** Customer filled a stamp card; redeemed at the cashier. */
    LOYALTY_CARD,
    /** Customer bought it with coins. */
    COIN,
}

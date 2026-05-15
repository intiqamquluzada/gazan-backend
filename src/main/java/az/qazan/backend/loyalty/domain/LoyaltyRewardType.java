package az.qazan.backend.loyalty.domain;

public enum LoyaltyRewardType {
    /** Customer gets the named item for free. */
    FREE_ITEM,

    /** Percentage off next purchase (rewardValue = 0..100). */
    PERCENTAGE_DISCOUNT,

    /** Fixed AZN off next purchase. */
    FIXED_DISCOUNT,

    /** Cash back to customer's wallet. */
    CASHBACK
}

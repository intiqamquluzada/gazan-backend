package az.qazan.backend.push.application;

/**
 * Which switch on the app's Notifications screen a push belongs to.
 * {@link #ALWAYS} is for account and work messages (business approved,
 * a customer at the counter) that no switch turns off.
 */
public enum PushCategory {
    /** Coins, stamps and gifts on the user's own cards. */
    REWARDS,
    /** Promotions from venues where the user holds a card. */
    OFFERS,
    /** Qazan-wide announcements. */
    MARKETING,
    ALWAYS
}

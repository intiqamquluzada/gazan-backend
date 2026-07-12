package az.qazan.backend.notifications.domain;

/** Audience a {@link Notification} is destined for. */
public enum NotificationTarget {
    /** Every user in the system. */
    BROADCAST,
    /** Exactly one user — see {@link Notification#getUserId()}. */
    USER,
    /** Every user holding a loyalty card at
     *  {@link Notification#getTargetCompanyId()}. */
    COMPANY_CARDHOLDERS;
}

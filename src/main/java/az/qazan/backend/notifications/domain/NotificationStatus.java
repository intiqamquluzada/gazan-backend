package az.qazan.backend.notifications.domain;

/** Lifecycle state for moderation of business-owner-submitted notifications. */
public enum NotificationStatus {
    /** Submitted by a business owner; waits in the admin's queue. */
    PENDING,
    /** Visible to recipients. Admin-created notifications start here. */
    APPROVED,
    /** Reviewed and refused by an admin. Kept for audit. */
    REJECTED;
}

package az.qazan.backend.user.domain;

/**
 * Top-level authorization role for a user.
 *
 * <p>One role per user keeps things simple. If a user needs more than
 * one capability later (e.g. a business owner who is also an admin) we
 * can promote this to a join table without touching the public API —
 * controllers depend on the {@link org.springframework.security.access.prepost.PreAuthorize}
 * authority strings, not the column itself.
 */
public enum Role {
    /** Default role created on register. */
    CUSTOMER,

    /** Owns or runs a business — sees the business-side dashboard. */
    BUSINESS_OWNER,

    /** Internal — full access to admin endpoints. */
    ADMIN;
}

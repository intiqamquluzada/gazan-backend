package az.qazan.backend.auth.api.dto;

/** One-time ticket to pass as {@code emailVerificationToken} on register. */
public record EmailVerifyResponse(String verificationToken) {
}

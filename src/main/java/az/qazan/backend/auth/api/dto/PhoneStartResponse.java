package az.qazan.backend.auth.api.dto;

/** {@code verificationRequired} turns true once SMS verification ships. */
public record PhoneStartResponse(boolean registered, boolean verificationRequired) {
}

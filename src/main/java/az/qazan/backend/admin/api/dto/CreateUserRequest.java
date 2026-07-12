package az.qazan.backend.admin.api.dto;

import az.qazan.backend.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin-issued account creation. {@link #role} defaults to
 * {@code BUSINESS_OWNER} on the service side when null, since that's
 * the most common admin use case (onboarding a new business).
 */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @Size(max = 32) String phone,
        Role role
) {
}

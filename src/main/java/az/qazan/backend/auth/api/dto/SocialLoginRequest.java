package az.qazan.backend.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SocialLoginRequest(
        @NotBlank @Pattern(regexp = "GOOGLE|APPLE") String provider,
        @NotBlank @Size(max = 8192) String idToken,
        @Size(max = 120) String fullName,
        /** Apple only: one-time code for the server-side token exchange. */
        @Size(max = 2048) String authorizationCode
) {
}

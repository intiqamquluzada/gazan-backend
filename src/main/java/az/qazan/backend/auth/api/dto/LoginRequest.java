package az.qazan.backend.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code identifier} wins over {@code email} and may be an email or phone. */
public record LoginRequest(
        @Size(max = 254)
        String identifier,

        @Email(message = "{validation.email.format}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}

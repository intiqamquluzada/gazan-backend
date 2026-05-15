package az.qazan.backend.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}

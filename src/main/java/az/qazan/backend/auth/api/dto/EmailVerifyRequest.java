package az.qazan.backend.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        String email,

        @NotBlank
        @Pattern(regexp = "^\\s*[0-9]{6}\\s*$")
        String code
) {
}

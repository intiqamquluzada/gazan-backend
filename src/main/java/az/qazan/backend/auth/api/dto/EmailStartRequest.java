package az.qazan.backend.auth.api.dto;

import az.qazan.backend.user.domain.AppLocale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sign-up step: mail a code to an address before the account exists. */
public record EmailStartRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        @Size(max = 254)
        String email,

        /** Language of the email; falls back to the request language. */
        AppLocale locale
) {
}

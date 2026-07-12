package az.qazan.backend.auth.api.dto;

import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        @Size(max = 254)
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 100, message = "{validation.password.size}")
        String password,

        @NotBlank(message = "{validation.user.full_name.required}")
        @Size(min = 2, max = 120)
        String fullName,

        @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "{validation.user.phone.format}")
        String phone,

        @Past(message = "{validation.user.birth_date.past}")
        LocalDate birthDate,

        Role role,

        AppLocale locale
) {
}

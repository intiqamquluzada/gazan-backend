package az.qazan.backend.user.api.dto;

import az.qazan.backend.user.domain.AppLocale;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * All fields are optional — only provided values overwrite. Email and
 * password are intentionally excluded; they have dedicated endpoints.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 120, message = "{validation.user.full_name.size}")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "{validation.user.phone.format}")
        String phone,

        @URL(message = "{validation.user.avatar.url}")
        @Size(max = 512)
        String avatarUrl,

        @Size(max = 120, message = "{validation.user.business_name.size}")
        String businessName,

        AppLocale locale
) {
}

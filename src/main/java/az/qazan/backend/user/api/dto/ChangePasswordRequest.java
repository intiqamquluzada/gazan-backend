package az.qazan.backend.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        /** Required unless the account has no password yet (Google / Apple). */
        String currentPassword,

        @NotBlank(message = "{validation.password.new.required}")
        @Size(min = 8, max = 100, message = "{validation.password.size}")
        String newPassword
) {
}

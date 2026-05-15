package az.qazan.backend.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "{validation.password.current.required}")
        String currentPassword,

        @NotBlank(message = "{validation.password.new.required}")
        @Size(min = 8, max = 100, message = "{validation.password.size}")
        String newPassword
) {
}

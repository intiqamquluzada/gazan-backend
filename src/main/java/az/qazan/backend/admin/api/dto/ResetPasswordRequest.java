package az.qazan.backend.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin-issued password overwrite. */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}

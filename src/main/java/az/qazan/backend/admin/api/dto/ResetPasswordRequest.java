package az.qazan.backend.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin-issued password overwrite. */
@io.swagger.v3.oas.annotations.media.Schema(name = "AdminResetPasswordRequest")
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}

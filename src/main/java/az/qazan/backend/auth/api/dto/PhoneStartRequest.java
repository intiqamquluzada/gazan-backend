package az.qazan.backend.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneStartRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "{validation.user.phone.format}")
        String phone
) {
}

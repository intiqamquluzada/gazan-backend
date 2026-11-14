package az.qazan.backend.business.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "{validation.user.phone.format}")
        String phone,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}

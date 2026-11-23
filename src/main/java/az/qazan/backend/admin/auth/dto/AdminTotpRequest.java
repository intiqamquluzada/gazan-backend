package az.qazan.backend.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTotpRequest(
        @NotBlank @Size(max = 1000) String challengeToken,
        @NotBlank @Pattern(regexp = "^\\s*[0-9]{3}\\s*[0-9]{3}\\s*$") String code
) {}

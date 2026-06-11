package az.qazan.backend.push.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnregisterDeviceTokenRequest(
        @NotBlank
        @Size(max = 512)
        String token
) {
}

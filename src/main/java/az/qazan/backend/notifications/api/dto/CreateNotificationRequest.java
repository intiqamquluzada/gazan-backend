package az.qazan.backend.notifications.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String body
) {
}

package az.qazan.backend.rewards.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SpendUseRequest(
        @NotNull @Pattern(regexp = "COIN|CARD") String kind,
        @NotNull UUID id
) {
}

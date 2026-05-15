package az.qazan.backend.loyalty.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JoinProgramRequest(@NotNull UUID programId) {
}

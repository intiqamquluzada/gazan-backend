package az.qazan.backend.nfc.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** The owner decides what a card does: stamp, points or spend. */
public record NfcAssignRequest(
        @NotNull String kind,
        UUID programId,
        @Min(1) @Max(100000) Integer pointsValue,
        @Min(1) @Max(10) Integer stampsValue,
        @Size(max = 80) String label,
        @Min(0) @Max(86400) Integer cooldownSec,
        @Min(1) @Max(100) Integer dailyCapPerUser
) {
}

package az.qazan.backend.nfc.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Partial update of a card's settings; only the fields sent change. */
public record NfcTagLimitsRequest(
        UUID programId,
        @Size(max = 80) String label,
        @Min(1) @Max(100000) Integer pointsValue,
        @Min(1) @Max(10) Integer stampsValue,
        @Min(0) @Max(86400) Integer cooldownSec,
        @Min(1) @Max(100) Integer dailyCapPerUser
) {
}

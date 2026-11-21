package az.qazan.backend.nfc.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sent by the CUSTOMER app right after its phone read a venue card.
 *
 * <p>{@code token} is what the tag carries. {@code tagUid} is the chip's
 * serial, {@code counter}/{@code cmac} stay empty for static NTAG 213 tags
 * and carry the SUN payload once DNA tags are rolled out.
 * {@code clientTapId} makes retries (offline queue) idempotent.
 */
public record NfcTapRequest(
        @NotBlank @Size(max = 64) String token,
        @Size(max = 32) String tagUid,
        Long counter,
        @Size(max = 64) String cmac,
        @Size(max = 64) String clientTapId,
        @Size(max = 80) String deviceId,
        Double latitude,
        Double longitude,
        /** Seconds between the physical tap and this request: 0 live, more for queued taps. */
        @jakarta.validation.constraints.Min(0) Integer ageSeconds
) {
}

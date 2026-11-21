package az.qazan.backend.nfc.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The owner holds a card to their phone to find out which one it is. */
public record NfcIdentifyRequest(
        @NotBlank @Size(max = 64) String token,
        @Size(max = 32) String tagUid
) {
}

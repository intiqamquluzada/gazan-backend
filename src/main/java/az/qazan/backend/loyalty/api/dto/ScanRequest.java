package az.qazan.backend.loyalty.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Sent by the business app after scanning a customer's QR.
 *
 * <p>The QR encodes the customer's user id. Business app passes the
 * intended program id and how many stamps to add (usually 1).
 */
public record ScanRequest(
        @NotNull UUID customerId,
        @NotNull UUID programId,
        @Min(1) int stamps,
        String note
) {
    public ScanRequest {
        if (stamps == 0) stamps = 1;
    }
}

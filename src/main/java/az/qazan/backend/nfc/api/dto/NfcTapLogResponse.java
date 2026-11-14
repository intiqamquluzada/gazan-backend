package az.qazan.backend.nfc.api.dto;

import az.qazan.backend.nfc.domain.NfcTap;

import java.time.Instant;
import java.util.UUID;

/** One row of the tap log (business + admin panels). */
public record NfcTapLogResponse(
        UUID id,
        UUID tagId,
        UUID userId,
        UUID companyId,
        String result,
        int pointsDelta,
        int stampsDelta,
        Integer distanceM,
        boolean suspicious,
        String note,
        Instant createdAt
) {
    public static NfcTapLogResponse of(NfcTap t) {
        return new NfcTapLogResponse(
                t.getId(), t.getTagId(), t.getUserId(), t.getCompanyId(),
                t.getResult().name(), t.getPointsDelta(), t.getStampsDelta(),
                t.getDistanceM(), t.isSuspicious(), t.getNote(), t.getCreatedAt());
    }
}

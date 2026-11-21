package az.qazan.backend.nfc.api.dto;

import az.qazan.backend.nfc.domain.NfcTag;

import java.time.Instant;
import java.util.UUID;

/** Card as shown in the admin / business panel. */
public record NfcTagResponse(
        UUID id,
        UUID companyId,
        String companyName,
        UUID programId,
        String programTitle,
        String kind,
        String label,
        String status,
        String tagUid,
        int pointsValue,
        int stampsValue,
        int cooldownSec,
        int dailyCapPerUser,
        Instant provisionedAt,
        Instant createdAt,
        /** Only returned once, when the card is created — this goes onto the tag. */
        String token
) {
    public static NfcTagResponse of(NfcTag t, boolean withToken) {
        return new NfcTagResponse(
                t.getId(),
                t.getCompany().getId(),
                t.getCompany().getName(),
                t.getProgram() == null ? null : t.getProgram().getId(),
                t.getProgram() == null ? null : t.getProgram().getTitle(),
                t.getKind().name(),
                t.getLabel(),
                t.getStatus().name(),
                t.getTagUid(),
                t.getPointsValue(),
                t.getStampsValue(),
                t.getCooldownSec(),
                t.getDailyCapPerUser(),
                t.getProvisionedAt(),
                t.getCreatedAt(),
                withToken ? t.getToken() : null);
    }
}

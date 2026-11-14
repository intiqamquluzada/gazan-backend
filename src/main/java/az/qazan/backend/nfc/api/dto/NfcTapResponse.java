package az.qazan.backend.nfc.api.dto;

import java.util.UUID;

/** What the customer app shows after a tap. */
public record NfcTapResponse(
        String kind,              // STAMP | POINTS | SPEND
        UUID companyId,
        String companyName,
        String programTitle,
        int stampsAdded,
        Integer stampsNow,
        Integer stampsRequired,
        int pointsAdded,
        /** Coins the customer holds AT THIS VENUE (the only ones spendable here). */
        long pointsBalance,
        boolean rewardUnlocked,
        UUID spendSessionId,      // SPEND
        /** Title of the gift this tap unlocked (stamp card completed), else null. */
        String unlockedRewardTitle
) {
}

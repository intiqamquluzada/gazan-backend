package az.qazan.backend.admin.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String role,
        boolean active,
        LocalDate birthDate,
        UUID staffCompanyId,
        Instant createdAt,
        Instant lastLoginAt,
        long coinBalance,
        List<Card> cards,
        List<AdminCoinTxnResponse> recentCoins
) {
    @io.swagger.v3.oas.annotations.media.Schema(name = "AdminUserCard")
    public record Card(
            UUID id,
            UUID companyId,
            String companyName,
            String programTitle,
            int stamps,
            int stampsRequired,
            int rewardsAvailable,
            int totalRewardsClaimed
    ) {}
}

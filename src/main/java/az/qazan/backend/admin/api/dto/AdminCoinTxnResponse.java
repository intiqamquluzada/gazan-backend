package az.qazan.backend.admin.api.dto;

import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record AdminCoinTxnResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        UUID companyId,
        String companyName,
        int amount,
        String type,
        String note,
        Instant createdAt
) {
    /** Must be called inside an open transaction — touches lazy relations. */
    public static AdminCoinTxnResponse from(CoinTransaction t) {
        User u = t.getUser();
        Company c = t.getCompany();
        return new AdminCoinTxnResponse(
                t.getId(),
                u == null ? null : u.getId(),
                u == null ? null : u.getFullName(),
                u == null ? null : u.getEmail(),
                c == null ? null : c.getId(),
                c == null ? null : c.getName(),
                t.getAmount(),
                t.getType().name(),
                t.getNote(),
                t.getCreatedAt()
        );
    }
}

package az.qazan.backend.admin.api.dto;

import az.qazan.backend.companies.domain.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminCompanyDetailResponse(
        UUID id,
        String name,
        String tagline,
        String category,
        boolean featured,
        Double rating,
        int reviewCount,
        String address,
        String phone,
        String country,
        String currency,
        String logoUrl,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        Instant createdAt,
        CompanyStatus status,
        long customers,
        long tags,
        long coinsEarned,
        long coinsSpent
) {
}

package az.qazan.backend.admin.api.dto;

import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record AdminCompanyResponse(
        UUID id,
        String name,
        String tagline,
        String category,
        boolean featured,
        Double rating,
        int reviewCount,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        Instant createdAt
) {
    /** Must be called inside an open transaction — touches the lazy owner. */
    public static AdminCompanyResponse from(Company c) {
        User owner = c.getOwner();
        return new AdminCompanyResponse(
                c.getId(),
                c.getName(),
                c.getTagline(),
                c.getCategory().name(),
                c.isFeatured(),
                c.getRating(),
                c.getReviewCount(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getEmail(),
                c.getCreatedAt()
        );
    }
}

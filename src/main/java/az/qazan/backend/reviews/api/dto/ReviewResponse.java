package az.qazan.backend.reviews.api.dto;

import az.qazan.backend.reviews.domain.ReviewView;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        String userName,
        int rating,
        String comment,
        Instant createdAt,
        boolean mine
) {
    public static ReviewResponse of(ReviewView v, UUID currentUserId) {
        return new ReviewResponse(
                v.getId(),
                v.getUserName(),
                v.getRating(),
                v.getComment(),
                v.getCreatedAt(),
                v.getUserId().equals(currentUserId)
        );
    }
}

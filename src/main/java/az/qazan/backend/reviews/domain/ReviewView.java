package az.qazan.backend.reviews.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Read projection joining a {@link Review} with its author's current name,
 * so the name always reflects the live (or anonymized) user.
 */
public interface ReviewView {
    UUID getId();
    String getUserName();
    int getRating();
    String getComment();
    Instant getCreatedAt();
    UUID getUserId();
}

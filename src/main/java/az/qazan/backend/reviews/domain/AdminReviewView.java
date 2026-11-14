package az.qazan.backend.reviews.domain;

import java.time.Instant;
import java.util.UUID;

/** Review joined with author + venue names, for the admin moderation list. */
public interface AdminReviewView {
    UUID getId();
    int getRating();
    String getComment();
    Instant getCreatedAt();
    String getUserName();
    UUID getCompanyId();
    String getCompanyName();
    long getReportCount();
}

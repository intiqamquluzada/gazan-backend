package az.qazan.backend.reviews.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

    long countByReviewId(UUID reviewId);
}

package az.qazan.backend.reviews.api.dto;

import java.util.List;

/**
 * Everything the company-detail screen needs: the aggregate rating, the
 * caller's own review (if any), and the full list.
 */
public record CompanyReviewsResponse(
        double average,
        long count,
        ReviewResponse myReview,
        List<ReviewResponse> reviews
) {
}

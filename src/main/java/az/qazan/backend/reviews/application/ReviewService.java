package az.qazan.backend.reviews.application;

import az.qazan.backend.reviews.api.dto.CompanyReviewsResponse;
import az.qazan.backend.reviews.api.dto.CreateReviewRequest;
import az.qazan.backend.reviews.api.dto.ReviewResponse;
import az.qazan.backend.reviews.domain.Review;
import az.qazan.backend.reviews.domain.ReviewRepository;
import az.qazan.backend.reviews.domain.ReviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;

    @Transactional(readOnly = true)
    public CompanyReviewsResponse forCompany(UUID companyId, UUID currentUserId) {
        List<ReviewView> views = reviews.findViewsByCompany(companyId);
        List<ReviewResponse> list = views.stream()
                .map(v -> ReviewResponse.of(v, currentUserId))
                .toList();
        ReviewResponse mine = list.stream()
                .filter(ReviewResponse::mine)
                .findFirst()
                .orElse(null);
        Double avg = reviews.averageForCompany(companyId);
        long count = reviews.countByCompanyId(companyId);
        return new CompanyReviewsResponse(
                avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0,
                count,
                mine,
                list
        );
    }

    /** Create or update the caller's review for this company. */
    @Transactional
    public void submit(UUID userId, UUID companyId, CreateReviewRequest req) {
        Review review = reviews.findByUserIdAndCompanyId(userId, companyId)
                .orElseGet(() -> Review.builder()
                        .userId(userId)
                        .companyId(companyId)
                        .build());
        review.setRating(req.rating());
        review.setComment(req.comment() == null || req.comment().isBlank()
                ? null : req.comment().trim());
        reviews.save(review);
    }
}

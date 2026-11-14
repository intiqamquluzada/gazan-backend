package az.qazan.backend.reviews.application;

import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.reviews.api.dto.CompanyReviewsResponse;
import az.qazan.backend.reviews.api.dto.CreateReviewRequest;
import az.qazan.backend.reviews.api.dto.ReviewResponse;
import az.qazan.backend.reviews.domain.Review;
import az.qazan.backend.reviews.domain.ReviewReport;
import az.qazan.backend.reviews.domain.ReviewReportReason;
import az.qazan.backend.reviews.domain.ReviewReportRepository;
import az.qazan.backend.reviews.domain.ReviewRepository;
import az.qazan.backend.common.exception.NotFoundException;
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
    private final CompanyRepository companies;
    private final ReviewReportRepository reports;

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
                list,
                currentUserId != null && canReview(currentUserId, companyId)
        );
    }

    /** Verified visitors only; owners can't review their own business. */
    private boolean canReview(UUID userId, UUID companyId) {
        return !reviews.isOwner(userId, companyId)
                && reviews.hasVisited(userId, companyId);
    }

    /** Create or update the caller's review for this company. */
    @Transactional
    public void submit(UUID userId, UUID companyId, CreateReviewRequest req) {
        if (reviews.isOwner(userId, companyId)) {
            throw new AppException(ErrorCode.REVIEW_OWN_BUSINESS);
        }
        if (!reviews.hasVisited(userId, companyId)) {
            throw new AppException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
        Review review = reviews.findByUserIdAndCompanyId(userId, companyId)
                .orElseGet(() -> Review.builder()
                        .userId(userId)
                        .companyId(companyId)
                        .build());
        review.setRating(req.rating());
        review.setComment(req.comment() == null || req.comment().isBlank()
                ? null : req.comment().trim());
        reviews.save(review);
        refreshCompanyRating(companyId);
    }

    /** Moderation delete; the venue's figures follow. */
    /**
     * A user flags someone else's review. One report per user and review;
     * a repeat is accepted silently so the app can retry safely.
     */
    @Transactional
    public void report(UUID userId, UUID reviewId, ReviewReportReason reason, String note) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (review.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.REVIEW_REPORT_OWN);
        }
        if (reports.existsByReviewIdAndUserId(reviewId, userId)) {
            return;
        }
        reports.save(ReviewReport.builder()
                .reviewId(reviewId)
                .userId(userId)
                .reason(reason)
                .note(note == null || note.isBlank() ? null : note.trim())
                .build());
    }

    @Transactional
    public void adminDelete(UUID reviewId) {
        reviews.findById(reviewId).ifPresent(review -> {
            reviews.delete(review);
            reviews.flush();
            refreshCompanyRating(review.getCompanyId());
        });
    }

    private void refreshCompanyRating(UUID companyId) {
        companies.findById(companyId).ifPresent(company -> {
            Double avg = reviews.averageForCompany(companyId);
            company.setRating(avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0);
            company.setReviewCount((int) reviews.countByCompanyId(companyId));
        });
    }
}

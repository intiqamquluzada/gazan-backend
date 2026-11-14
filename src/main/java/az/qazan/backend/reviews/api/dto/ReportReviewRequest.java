package az.qazan.backend.reviews.api.dto;

import az.qazan.backend.reviews.domain.ReviewReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST /api/v1/reviews/{id}/report. */
public record ReportReviewRequest(
        @NotNull ReviewReportReason reason,
        @Size(max = 500) String note
) {
}

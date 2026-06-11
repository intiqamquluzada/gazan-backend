package az.qazan.backend.reviews.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @Min(value = 1, message = "{validation.review.rating.range}")
        @Max(value = 5, message = "{validation.review.rating.range}")
        int rating,

        @Size(max = 1000)
        String comment
) {
}

package az.qazan.backend.reviews.api;

import az.qazan.backend.common.api.ApiResponse;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.reviews.api.dto.ReportReviewRequest;
import az.qazan.backend.reviews.application.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Any signed-in user can flag someone else's review for moderation. */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
@SecurityRequirement(name = "bearerAuth")
public class ReviewReportController {

    private final ReviewService service;

    @Operation(summary = "Report a review (one report per user; repeats are ignored)")
    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> report(@CurrentUser AppUserPrincipal me,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody ReportReviewRequest body) {
        service.report(me.getId(), id, body.reason(), body.note());
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }
}

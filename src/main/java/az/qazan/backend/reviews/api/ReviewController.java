package az.qazan.backend.reviews.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.reviews.api.dto.CompanyReviewsResponse;
import az.qazan.backend.reviews.api.dto.CreateReviewRequest;
import az.qazan.backend.reviews.application.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Business ratings & reviews")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviews;

    @Operation(summary = "Aggregate rating + the caller's review + full list")
    @GetMapping
    public CompanyReviewsResponse list(
            @PathVariable UUID companyId,
            @CurrentUser AppUserPrincipal me) {
        return reviews.forCompany(companyId, me.getId());
    }

    @Operation(summary = "Create or update the caller's review for this business")
    @PostMapping
    public ResponseEntity<Void> submit(
            @PathVariable UUID companyId,
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody CreateReviewRequest body) {
        reviews.submit(me.getId(), companyId, body);
        return ResponseEntity.noContent().build();
    }
}

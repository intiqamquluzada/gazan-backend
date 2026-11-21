package az.qazan.backend.reviews.api;

import az.qazan.backend.reviews.application.ReviewService;
import az.qazan.backend.reviews.domain.AdminReviewView;
import az.qazan.backend.reviews.domain.ReviewRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Admin review moderation: newest reviews across all venues + delete. */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewsController {

    private final ReviewRepository reviews;
    private final ReviewService service;

    @Operation(summary = "Latest reviews across all businesses")
    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminReviewView> latest(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean reported) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        return reported ? reviews.findReportedForAdmin(page) : reviews.findLatestForAdmin(page);
    }

    @Operation(summary = "Delete a review (moderation)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.adminDelete(id);
    }
}

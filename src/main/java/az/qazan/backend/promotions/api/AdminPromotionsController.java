package az.qazan.backend.promotions.api;

import az.qazan.backend.promotions.api.dto.AdminPromotionRequest;
import az.qazan.backend.promotions.api.dto.AdminPromotionResponse;
import az.qazan.backend.promotions.application.PromotionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD for the Discover promotion banners ("reklamlar"). Read
 * endpoints for the customer feed live in {@link PromotionsController};
 * these mutate and are ADMIN-only.
 */
@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionsController {

    private final PromotionsService service;

    @Operation(summary = "List all promotions (active + inactive)")
    @GetMapping
    public List<AdminPromotionResponse> list() {
        return service.allForAdmin().stream()
                .map(AdminPromotionResponse::from).toList();
    }

    @Operation(summary = "Create a promotion")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPromotionResponse create(
            @Valid @RequestBody AdminPromotionRequest body) {
        return AdminPromotionResponse.from(service.create(body));
    }

    @Operation(summary = "Update a promotion")
    @PutMapping("/{id}")
    public AdminPromotionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminPromotionRequest body) {
        return AdminPromotionResponse.from(service.update(id, body));
    }

    @Operation(summary = "Activate / deactivate a promotion")
    @PatchMapping("/{id}/active")
    public AdminPromotionResponse setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        return AdminPromotionResponse.from(service.setActive(id, active));
    }

    @Operation(summary = "Delete a promotion")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

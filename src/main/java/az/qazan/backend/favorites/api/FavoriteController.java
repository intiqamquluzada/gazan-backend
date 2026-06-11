package az.qazan.backend.favorites.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.favorites.application.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Customer's saved businesses")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favorites;

    @Operation(summary = "List the signed-in user's favorited company IDs")
    @GetMapping
    public List<UUID> mine(@CurrentUser AppUserPrincipal me) {
        return favorites.listCompanyIds(me.getId());
    }

    @Operation(summary = "Add a company to favorites")
    @PostMapping("/{companyId}")
    public ResponseEntity<Void> add(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID companyId) {
        favorites.add(me.getId(), companyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a company from favorites")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> remove(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID companyId) {
        favorites.remove(me.getId(), companyId);
        return ResponseEntity.noContent().build();
    }
}

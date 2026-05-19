package az.qazan.backend.admin.api;

import az.qazan.backend.admin.api.dto.AdminStatsResponse;
import az.qazan.backend.admin.application.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final AdminStatsService stats;

    @Operation(summary = "Platform-wide dashboard snapshot (admin only)")
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return stats.overview();
    }
}

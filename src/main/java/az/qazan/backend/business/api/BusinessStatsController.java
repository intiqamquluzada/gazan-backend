package az.qazan.backend.business.api;

import az.qazan.backend.business.api.dto.BusinessAnalyticsResponse;
import az.qazan.backend.business.api.dto.BusinessStatsResponse;
import az.qazan.backend.business.application.BusinessAnalyticsService;
import az.qazan.backend.business.application.BusinessStatsService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Tag(name = "Business")
@SecurityRequirement(name = "bearerAuth")
public class BusinessStatsController {

    private final BusinessStatsService stats;
    private final BusinessAnalyticsService analytics;

    @Operation(summary = "Aggregated stats for the signed-in business owner's dashboard")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @GetMapping("/stats")
    public BusinessStatsResponse stats(@CurrentUser AppUserPrincipal me) {
        return stats.statsForOwner(me.getId());
    }

    @Operation(summary = "Campaign and customer analytics for the last N days (max 90)")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @GetMapping("/analytics")
    public BusinessAnalyticsResponse analytics(
            @CurrentUser AppUserPrincipal me,
            @RequestParam(defaultValue = "30") int days) {
        return analytics.forOwner(me.getId(), days);
    }
}

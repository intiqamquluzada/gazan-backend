package az.qazan.backend.admin.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analytics;

    @Operation(summary = "Headline figures for a period, beside the period before it")
    @GetMapping("/overview")
    public AnalyticsDtos.Overview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.overview(from, to);
    }

    @Operation(summary = "Activity per day, week or month")
    @GetMapping("/timeseries")
    public AnalyticsDtos.Series timeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AnalyticsDtos.Bucket bucket) {
        return analytics.timeseries(from, to, bucket);
    }

    @Operation(summary = "Splits, leaderboards and the weekly activity heatmap")
    @GetMapping("/breakdown")
    public AnalyticsDtos.Breakdown breakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.breakdown(from, to);
    }
}

package az.qazan.backend.admin.api;

import az.qazan.backend.admin.api.dto.AdminAdjustCoinsRequest;
import az.qazan.backend.admin.api.dto.AdminCoinTxnResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.application.AdminCoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/coins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminCoinsController {

    private final AdminCoinService coins;

    @Operation(summary = "Platform-wide coin ledger, newest first (admin only)")
    @GetMapping("/transactions")
    public PageResponse<AdminCoinTxnResponse> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return coins.transactions(page, size);
    }

    @Operation(summary = "Aggregate coin totals (admin only)")
    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return coins.summary();
    }

    @Operation(summary = "Manually adjust a user's coin balance (admin only)")
    @PostMapping("/adjust")
    public AdminCoinTxnResponse adjust(@Valid @RequestBody AdminAdjustCoinsRequest body) {
        return coins.adjust(
                body.userId(), body.companyId(), body.amount(), body.note());
    }
}

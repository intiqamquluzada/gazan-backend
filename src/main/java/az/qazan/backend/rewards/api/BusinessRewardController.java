package az.qazan.backend.rewards.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.rewards.api.dto.RewardClaimResponse;
import az.qazan.backend.rewards.api.dto.UseRewardRequest;
import az.qazan.backend.rewards.application.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints the cashier (business owner) uses after scanning a
 * customer's QR. Auth-checked at the service layer too (company
 * ownership), so {@code @PreAuthorize} here is the first line of
 * defence.
 */
@RestController
@RequestMapping("/api/v1/business/rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
@Tag(name = "Rewards")
@SecurityRequirement(name = "bearerAuth")
public class BusinessRewardController {

    private final RewardService rewards;

    @Operation(summary = "Active vouchers a customer can use at MY company")
    @GetMapping("/customers/{customerId}/active-at/{companyId}")
    public List<RewardClaimResponse> activeAtCompany(
            @PathVariable UUID customerId,
            @PathVariable UUID companyId
    ) {
        return rewards.activeAtCompany(customerId, companyId);
    }

    @Operation(summary = "Mark a voucher used (cashier confirms claim)")
    @PostMapping("/use")
    public RewardClaimResponse use(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody UseRewardRequest body
    ) {
        return rewards.use(me.getId(), body.kind(), body.id(), body.customerId());
    }
}

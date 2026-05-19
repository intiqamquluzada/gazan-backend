package az.qazan.backend.coins.api;

import az.qazan.backend.coins.api.dto.CoinSummaryResponse;
import az.qazan.backend.coins.api.dto.GrantCoinsRequest;
import az.qazan.backend.coins.api.dto.RedeemRewardRequest;
import az.qazan.backend.coins.api.dto.RedeemResultResponse;
import az.qazan.backend.coins.api.dto.SpendCoinsRequest;
import az.qazan.backend.coins.application.CoinService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * The coin wallet: a single balance the customer grows at every business
 * and spends on rewards / cash discounts.
 */
@RestController
@RequestMapping("/api/v1/coins")
@RequiredArgsConstructor
@Tag(name = "Coins")
@SecurityRequirement(name = "bearerAuth")
public class CoinController {

    private final CoinService coins;

    @Operation(summary = "Coin balance, per-business breakdown and recent activity")
    @GetMapping("/me")
    public CoinSummaryResponse me(@CurrentUser AppUserPrincipal user) {
        return coins.summary(user.getId());
    }

    @Operation(summary = "Spend coins on a reward or cash discount")
    @PostMapping("/me/spend")
    public CoinSummaryResponse spend(
            @CurrentUser AppUserPrincipal user,
            @Valid @RequestBody SpendCoinsRequest body
    ) {
        return coins.spend(user.getId(), body.companyId(), body.amount(), body.note());
    }

    @Operation(summary = "Business owner credits coins to a customer (after a scan)")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping("/grant")
    public CoinSummaryResponse grant(@Valid @RequestBody GrantCoinsRequest body) {
        return coins.grant(
                body.customerId(), body.companyId(), body.amount(), body.note());
    }

    @Operation(summary = "Cashier confirms a customer redeeming a coin reward")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping("/redeem-reward")
    public RedeemResultResponse redeemReward(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody RedeemRewardRequest body
    ) {
        return coins.redeemReward(
                me.getId(), body.customerId(), body.rewardId());
    }
}

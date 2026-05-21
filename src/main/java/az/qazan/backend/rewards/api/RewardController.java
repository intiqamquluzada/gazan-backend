package az.qazan.backend.rewards.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.rewards.api.dto.PurchaseRewardRequest;
import az.qazan.backend.rewards.api.dto.RewardClaimResponse;
import az.qazan.backend.rewards.application.RewardService;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards")
@SecurityRequirement(name = "bearerAuth")
public class RewardController {

    private final RewardService rewards;

    @Operation(summary = "My vouchers (coin claims + card-completed entries)")
    @GetMapping("/mine")
    public List<RewardClaimResponse> mine(
            @CurrentUser AppUserPrincipal me,
            @RequestParam(required = false) RewardClaimStatus status
    ) {
        return rewards.mine(me.getId(), status);
    }

    @Operation(summary = "Buy a coin reward — deducts coins, creates ACTIVE voucher")
    @PostMapping("/purchase")
    public RewardClaimResponse purchase(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody PurchaseRewardRequest body
    ) {
        return rewards.purchase(me.getId(), body.coinRewardId());
    }
}

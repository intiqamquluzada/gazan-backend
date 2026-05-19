package az.qazan.backend.coins.api;

import az.qazan.backend.coins.api.dto.CoinRewardResponse;
import az.qazan.backend.coins.api.dto.CreateCoinRewardRequest;
import az.qazan.backend.coins.application.CoinRewardService;
import az.qazan.backend.coins.domain.CoinReward;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Per-business catalog of rewards a customer can claim with coins. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Coin rewards")
@SecurityRequirement(name = "bearerAuth")
public class CoinRewardController {

    private final CoinRewardService service;

    @Operation(summary = "List a company's coin rewards")
    @GetMapping("/companies/{companyId}/coin-rewards")
    public List<CoinRewardResponse> list(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return service.listForCompany(companyId, activeOnly).stream()
                .map(CoinRewardController::toResponse).toList();
    }

    @Operation(summary = "Create a coin reward (business owner)")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping("/companies/{companyId}/coin-rewards")
    @ResponseStatus(HttpStatus.CREATED)
    public CoinRewardResponse create(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateCoinRewardRequest body
    ) {
        return toResponse(service.create(
                companyId, me.getId(),
                body.title(), body.description(), body.coinCost()));
    }

    @Operation(summary = "Delete a coin reward (business owner)")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @DeleteMapping("/coin-rewards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        service.delete(id, me.getId());
    }

    private static CoinRewardResponse toResponse(CoinReward r) {
        return new CoinRewardResponse(
                r.getId(),
                r.getCompany().getId(),
                r.getTitle(),
                r.getDescription(),
                r.getCoinCost(),
                r.isActive());
    }
}

package az.qazan.backend.rewards.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.rewards.api.dto.SpendSessionResponse;
import az.qazan.backend.rewards.api.dto.SpendUseRequest;
import az.qazan.backend.rewards.application.SpendSessionService;
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
import java.util.Map;
import java.util.UUID;

/** The cashier's live list of customers who just read the SPEND card. */
@RestController
@RequestMapping("/api/v1/business/spend-sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('CASHIER')")
@Tag(name = "Rewards")
@SecurityRequirement(name = "bearerAuth")
public class BusinessSpendSessionController {

    private final SpendSessionService sessions;

    @Operation(summary = "Open customer windows at my venue, newest first")
    @GetMapping
    public List<SpendSessionResponse> open(@CurrentUser AppUserPrincipal me) {
        return sessions.open(me.getId());
    }

    @Operation(summary = "Mark one reward in this window as used")
    @PostMapping("/{id}/use")
    public SpendSessionResponse use(@CurrentUser AppUserPrincipal me,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody SpendUseRequest body) {
        return sessions.use(me.getId(), id, body.kind(), body.id());
    }

    @Operation(summary = "Close this window")
    @PostMapping("/{id}/close")
    public Map<String, String> close(@CurrentUser AppUserPrincipal me,
                                     @PathVariable UUID id) {
        sessions.close(me.getId(), id);
        return Map.of("status", "COMPLETED");
    }
}

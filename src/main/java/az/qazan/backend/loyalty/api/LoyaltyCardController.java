package az.qazan.backend.loyalty.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.loyalty.api.dto.JoinProgramRequest;
import az.qazan.backend.loyalty.api.dto.LoyaltyCardResponse;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.application.LoyaltyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty/cards")
@RequiredArgsConstructor
@Tag(name = "Loyalty cards")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyCardController {

    private final LoyaltyCardService cards;
    private final LoyaltyMapper mapper;

    @Operation(summary = "All loyalty cards belonging to the signed-in user")
    @GetMapping("/me")
    public List<LoyaltyCardResponse> myCards(@CurrentUser AppUserPrincipal me) {
        return cards.myCards(me.getId()).stream().map(mapper::toCardResponse).toList();
    }

    @Operation(summary = "Join a loyalty program (creates a fresh card if not joined yet)")
    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public LoyaltyCardResponse join(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody JoinProgramRequest body
    ) {
        return mapper.toCardResponse(cards.joinProgram(me.getId(), body.programId()));
    }

    @Operation(summary = "Redeem one available reward on the given card")
    @PostMapping("/{id}/redeem")
    public LoyaltyCardResponse redeem(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id
    ) {
        return mapper.toCardResponse(cards.redeem(me.getId(), id));
    }
}

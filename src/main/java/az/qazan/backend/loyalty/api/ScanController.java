package az.qazan.backend.loyalty.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.loyalty.api.dto.LoyaltyCardResponse;
import az.qazan.backend.loyalty.api.dto.ScanRequest;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.application.LoyaltyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
@Tag(name = "Scans")
@SecurityRequirement(name = "bearerAuth")
public class ScanController {

    private final LoyaltyCardService cards;
    private final LoyaltyMapper mapper;

    @Operation(summary = "Business scans customer QR — adds stamp(s) to that customer's card")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping
    public LoyaltyCardResponse scan(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody ScanRequest body
    ) {
        return mapper.toCardResponse(cards.scan(
                me.getId(),
                body.customerId(),
                body.programId(),
                body.stamps(),
                body.note()
        ));
    }
}

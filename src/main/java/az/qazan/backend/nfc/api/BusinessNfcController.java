package az.qazan.backend.nfc.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.nfc.api.dto.NfcAssignRequest;
import az.qazan.backend.nfc.api.dto.NfcIdentifyRequest;
import az.qazan.backend.nfc.api.dto.NfcTagLimitsRequest;
import az.qazan.backend.nfc.api.dto.NfcTagResponse;
import az.qazan.backend.nfc.api.dto.NfcTapLogResponse;
import az.qazan.backend.nfc.application.NfcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The owner sees and tunes THEIR OWN cards: block a lost card, change how
 * many points a tap gives, the cooldown and the daily cap, and read the
 * tap log. Issuing new cards stays with the admin.
 */
@RestController
@RequestMapping("/api/v1/business/nfc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
@Tag(name = "NFC")
@SecurityRequirement(name = "bearerAuth")
public class BusinessNfcController {

    private final NfcService nfc;
    private final CompanyService companies;

    @Operation(summary = "My venue's NFC cards")
    @GetMapping("/tags")
    public List<NfcTagResponse> myTags(@CurrentUser AppUserPrincipal me) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        return nfc.forCompany(companyId);
    }

    @Operation(summary = "Block / unblock one of my cards")
    @PatchMapping("/tags/{id}/status")
    public NfcTagResponse status(@CurrentUser AppUserPrincipal me,
                                 @PathVariable UUID id,
                                 @RequestParam String value) {
        assertMine(me, id);
        return nfc.setStatusByOwner(id, value);
    }

    @Operation(summary = "Points per tap, cooldown and daily cap for one of my cards")
    @PatchMapping("/tags/{id}")
    public NfcTagResponse limits(@CurrentUser AppUserPrincipal me,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody NfcTagLimitsRequest body) {
        assertMine(me, id);
        return nfc.updateLimits(id, body);
    }

    @Operation(summary = "Read a card with my phone to find out which one it is")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    @PostMapping("/identify")
    public NfcTagResponse identify(@CurrentUser AppUserPrincipal me,
                                   @Valid @RequestBody NfcIdentifyRequest body) {
        return nfc.identify(me.getId(), body.token(), body.tagUid());
    }

    @Operation(summary = "Decide what one of my cards does")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    @PatchMapping("/tags/{id}/assign")
    public NfcTagResponse assign(@CurrentUser AppUserPrincipal me,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody NfcAssignRequest body) {
        assertMine(me, id);
        return nfc.assign(id, body);
    }

    @Operation(summary = "Tap log for my venue")
    @GetMapping("/taps")
    public List<NfcTapLogResponse> taps(@CurrentUser AppUserPrincipal me,
                                        @RequestParam(defaultValue = "7") int days,
                                        @RequestParam(defaultValue = "100") int limit) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        return nfc.companyTaps(companyId, days, limit).stream()
                .map(NfcTapLogResponse::of).toList();
    }

    private void assertMine(AppUserPrincipal me, UUID tagId) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        if (!nfc.companyIdOf(tagId).equals(companyId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}

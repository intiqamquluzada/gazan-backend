package az.qazan.backend.nfc.api;

import az.qazan.backend.admin.application.AdminAuditService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.nfc.api.dto.NfcTagLimitsRequest;
import az.qazan.backend.nfc.api.dto.NfcTagRequest;
import az.qazan.backend.nfc.api.dto.NfcTagResponse;
import az.qazan.backend.nfc.api.dto.NfcTapLogResponse;
import az.qazan.backend.nfc.application.NfcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin card inventory. The token is returned ONLY by create/rotate — that
 * is the moment it gets written onto the physical tag.
 */
@RestController
@RequestMapping("/api/v1/admin/nfc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminNfcController {

    private final NfcService nfc;
    private final AdminAuditService audit;

    @Operation(summary = "All NFC cards (optionally one venue's)")
    @GetMapping("/tags")
    public List<NfcTagResponse> list(@RequestParam(required = false) UUID companyId) {
        return companyId == null ? nfc.all() : nfc.forCompany(companyId);
    }

    @Operation(summary = "Issue a card — response carries the token to write on the tag")
    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public NfcTagResponse create(@Valid @RequestBody NfcTagRequest body) {
        return nfc.issue(body);
    }

    @Operation(summary = "New secret for a card (lost / copied) — rewrite the tag after this")
    @PostMapping("/tags/{id}/rotate")
    public NfcTagResponse rotate(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        audit.record(me.getId(), "NFC_ROTATE", "NFC_TAG", id, null);
        return nfc.rotateToken(id);
    }

    @Operation(summary = "Mark a card as written to its physical tag")
    @PostMapping("/tags/{id}/provisioned")
    public NfcTagResponse provisioned(@PathVariable UUID id) {
        return nfc.markProvisioned(id);
    }

    @Operation(summary = "Block / unblock a card")
    @PatchMapping("/tags/{id}/status")
    public NfcTagResponse status(@CurrentUser AppUserPrincipal me,
                                 @PathVariable UUID id,
                                 @RequestParam String value) {
        NfcTagResponse updated = nfc.setStatus(id, value);
        audit.record(me.getId(), "NFC_STATUS", "NFC_TAG", id, updated.status());
        return updated;
    }

    @Operation(summary = "Card settings (points per tap, cooldown, daily cap, campaign)")
    @PatchMapping("/tags/{id}")
    public NfcTagResponse update(@PathVariable UUID id,
                                 @Valid @RequestBody NfcTagLimitsRequest body) {
        return nfc.updateLimits(id, body);
    }

    @Operation(summary = "Latest taps across all venues (abuse monitoring)")
    @GetMapping("/taps")
    public List<NfcTapLogResponse> taps(
            @RequestParam(defaultValue = "false") boolean suspicious,
            @RequestParam(defaultValue = "200") int limit) {
        return nfc.latestTaps(suspicious, limit).stream()
                .map(NfcTapLogResponse::of).toList();
    }
}

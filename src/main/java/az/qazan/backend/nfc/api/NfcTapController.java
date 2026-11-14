package az.qazan.backend.nfc.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.nfc.api.dto.NfcTapRequest;
import az.qazan.backend.nfc.api.dto.NfcTapResponse;
import az.qazan.backend.nfc.application.NfcService;
import az.qazan.backend.rewards.api.dto.CustomerSpendSessionResponse;
import az.qazan.backend.rewards.application.SpendSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The customer's phone posts here after reading a venue's NFC card. */
@RestController
@RequestMapping("/api/v1/nfc")
@RequiredArgsConstructor
@Tag(name = "NFC")
@SecurityRequirement(name = "bearerAuth")
public class NfcTapController {

    private final NfcService nfc;
    private final SpendSessionService spendSessions;

    @Operation(summary = "Register a tap on a venue card (stamp or points)")
    @PostMapping("/tap")
    public NfcTapResponse tap(@CurrentUser AppUserPrincipal me,
                              @Valid @RequestBody NfcTapRequest body) {
        return nfc.tap(me.getId(), body);
    }

    @Operation(summary = "My own window at the till, so the phone can follow along")
    @GetMapping("/spend-sessions/{id}")
    public CustomerSpendSessionResponse spendSession(@CurrentUser AppUserPrincipal me,
                                                     @PathVariable UUID id) {
        return spendSessions.forCustomer(me.getId(), id);
    }
}

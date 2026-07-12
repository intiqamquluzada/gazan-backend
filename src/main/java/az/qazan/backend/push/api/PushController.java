package az.qazan.backend.push.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.push.api.dto.RegisterDeviceTokenRequest;
import az.qazan.backend.push.api.dto.UnregisterDeviceTokenRequest;
import az.qazan.backend.push.application.PushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "Push", description = "Device push-token registration")
@SecurityRequirement(name = "bearerAuth")
public class PushController {

    private final PushService push;

    @Operation(summary = "Register/refresh this device's push token")
    @PostMapping("/tokens")
    public ResponseEntity<Void> register(
            @CurrentUser AppUserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequest body) {
        push.register(principal.getId(), body.token(), body.platform());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unregister this device's push token (on sign-out)")
    @DeleteMapping("/tokens")
    public ResponseEntity<Void> unregister(
            @Valid @RequestBody UnregisterDeviceTokenRequest body) {
        push.unregister(body.token());
        return ResponseEntity.noContent().build();
    }
}

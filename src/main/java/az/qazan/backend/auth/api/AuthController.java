package az.qazan.backend.auth.api;

import az.qazan.backend.auth.api.dto.AuthResponse;
import az.qazan.backend.auth.api.dto.LoginRequest;
import az.qazan.backend.auth.api.dto.RefreshRequest;
import az.qazan.backend.auth.api.dto.RegisterRequest;
import az.qazan.backend.auth.application.AuthService;
import az.qazan.backend.common.api.ApiResponse;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, login, token refresh, logout")
public class AuthController {

    private final AuthService auth;
    private final UserService users;

    @Operation(summary = "Create a new account and return a token pair")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest body,
                                 HttpServletRequest http) {
        return auth.register(body, http);
    }

    @Operation(summary = "Exchange credentials for a token pair")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest http) {
        return auth.login(body, http);
    }

    @Operation(summary = "Rotate a refresh token for a fresh pair")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest body,
                                HttpServletRequest http) {
        return auth.refresh(body.refreshToken(), http);
    }

    @Operation(summary = "Revoke the supplied refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest body) {
        auth.logout(body.refreshToken());
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }

    @Operation(summary = "Revoke every refresh token belonging to the signed-in user")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@CurrentUser AppUserPrincipal me) {
        auth.logoutAllSessions(users.getById(me.getId()));
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }
}

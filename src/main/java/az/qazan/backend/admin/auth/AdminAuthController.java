package az.qazan.backend.admin.auth;

import az.qazan.backend.admin.auth.dto.AdminLoginRequest;
import az.qazan.backend.admin.auth.dto.AdminLoginResponse;
import az.qazan.backend.admin.auth.dto.AdminSessionResponse;
import az.qazan.backend.admin.auth.dto.AdminTotpRequest;
import az.qazan.backend.common.api.ApiResponse;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Sign-in for the web admin panel. The refresh token lives in an httpOnly
 * cookie scoped to this path; the cookie-only routes also demand a custom
 * header, which a cross-site form can't send.
 */
@RestController
@RequestMapping(AdminAuthController.BASE)
@Tag(name = "Admin auth", description = "Web admin panel: password + authenticator code")
public class AdminAuthController {

    static final String BASE = "/api/v1/admin/auth";
    public static final String COOKIE = "qazan_admin_rt";
    public static final String CSRF_HEADER = "X-Qazan-Admin";

    private final AdminAuthService auth;
    private final boolean secureCookie;
    private final String sameSite;

    public AdminAuthController(AdminAuthService auth,
                               @Value("${qazan.admin.cookie-secure:true}") boolean secureCookie,
                               @Value("${qazan.admin.cookie-same-site:Strict}") String sameSite) {
        this.auth = auth;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    @Operation(summary = "Step 1: email and password; returns a challenge for the code step")
    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest body) {
        return auth.login(body.email(), body.password());
    }

    @Operation(summary = "Step 2: authenticator code; opens the session")
    @PostMapping("/verify")
    public AdminSessionResponse verify(@Valid @RequestBody AdminTotpRequest body,
                                       HttpServletRequest http, HttpServletResponse res) {
        AdminAuthService.Session s = auth.verify(body.challengeToken(), body.code(), http);
        setCookie(res, s.refreshToken(), auth.idleTimeout());
        return s.body();
    }

    @Operation(summary = "Rotate the session cookie for a fresh access token")
    @PostMapping("/refresh")
    public AdminSessionResponse refresh(
            @CookieValue(name = COOKIE, required = false) String cookie,
            @RequestHeader(name = CSRF_HEADER, required = false) String marker,
            HttpServletRequest http, HttpServletResponse res) {
        requireMarker(marker);
        try {
            AdminAuthService.Session s = auth.refresh(cookie, http);
            setCookie(res, s.refreshToken(), auth.idleTimeout());
            return s.body();
        } catch (UnauthorizedException e) {
            setCookie(res, "", Duration.ZERO);
            throw e;
        }
    }

    @Operation(summary = "End the session and clear the cookie")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = COOKIE, required = false) String cookie,
            @RequestHeader(name = CSRF_HEADER, required = false) String marker,
            HttpServletResponse res) {
        requireMarker(marker);
        auth.logout(cookie);
        setCookie(res, "", Duration.ZERO);
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }

    @Operation(summary = "The signed-in admin")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/me")
    public AdminSessionResponse.AdminProfile me(@CurrentUser AppUserPrincipal me) {
        return auth.profile(me.getId());
    }

    private static void requireMarker(String marker) {
        if (!"1".equals(marker)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
    }

    private void setCookie(HttpServletResponse res, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(BASE)
                .maxAge(maxAge)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

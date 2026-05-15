package az.qazan.backend.user.api;

import az.qazan.backend.common.api.ApiResponse;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.user.api.dto.ChangePasswordRequest;
import az.qazan.backend.user.api.dto.UpdateProfileRequest;
import az.qazan.backend.user.api.dto.UserResponse;
import az.qazan.backend.user.application.UserMapper;
import az.qazan.backend.user.application.UserService;
import az.qazan.backend.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService users;
    private final UserMapper mapper;

    @Operation(summary = "Get the signed-in user's profile")
    @GetMapping("/me")
    public UserResponse me(@CurrentUser AppUserPrincipal principal) {
        User u = users.getById(principal.getId());
        return mapper.toResponse(u);
    }

    @Operation(summary = "Edit profile (name, phone, avatar, business name, locale)")
    @PutMapping("/me")
    public UserResponse updateMe(
            @CurrentUser AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest body) {
        User u = users.updateProfile(principal.getId(), body);
        return mapper.toResponse(u);
    }

    @Operation(summary = "Change password")
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest body) {
        users.changePassword(principal.getId(), body);
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }

    @Operation(summary = "Deactivate (soft-delete) the signed-in account")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivate(@CurrentUser AppUserPrincipal principal) {
        users.deactivate(principal.getId());
        return ResponseEntity.noContent().build();
    }
}

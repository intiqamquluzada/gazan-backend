package az.qazan.backend.admin.api;

import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.api.dto.UpdateUserRoleRequest;
import az.qazan.backend.admin.api.dto.UpdateUserStatusRequest;
import az.qazan.backend.admin.application.AdminUserService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminUsersController {

    private final AdminUserService users;

    @Operation(summary = "Search/paginate all users (admin only)")
    @GetMapping
    public PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return users.list(q, role, page, size);
    }

    @Operation(summary = "Change a user's role (admin only)")
    @PatchMapping("/{id}/role")
    public AdminUserResponse changeRole(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest body
    ) {
        return users.changeRole(me.getId(), id, body.role());
    }

    @Operation(summary = "Activate or block a user (admin only)")
    @PatchMapping("/{id}/status")
    public AdminUserResponse changeStatus(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest body
    ) {
        return users.changeStatus(me.getId(), id, body.active());
    }
}

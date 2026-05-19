package az.qazan.backend.admin.api.dto;

import az.qazan.backend.user.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull Role role) {
}

package az.qazan.backend.admin.api;

import az.qazan.backend.admin.api.dto.AdminAuditResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.application.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuditController {

    private final AdminAuditService audit;

    @Operation(summary = "Admin actions, newest first")
    @GetMapping
    public PageResponse<AdminAuditResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return audit.list(page, size);
    }
}

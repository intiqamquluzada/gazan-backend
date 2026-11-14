package az.qazan.backend.admin.api;

import az.qazan.backend.admin.api.dto.AdminCompanyDetailResponse;
import az.qazan.backend.admin.api.dto.AdminCompanyResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.api.dto.UpdateCompanyFeaturedRequest;
import az.qazan.backend.admin.api.dto.UpdateCompanyStatusRequest;
import az.qazan.backend.admin.application.AdminCompanyService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.companies.domain.CompanyStatus;
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
@RequestMapping("/api/v1/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminCompaniesController {

    private final AdminCompanyService companies;

    @Operation(summary = "Search/paginate all businesses (admin only)")
    @GetMapping
    public PageResponse<AdminCompanyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return companies.list(q, status, page, size);
    }

    @Operation(summary = "One business with its totals (admin only)")
    @GetMapping("/{id}")
    public AdminCompanyDetailResponse detail(@PathVariable UUID id) {
        return companies.detail(id);
    }

    @Operation(summary = "Approve or suspend a business (admin only)")
    @PatchMapping("/{id}/status")
    public AdminCompanyResponse setStatus(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyStatusRequest body
    ) {
        return companies.setStatus(me.getId(), id, body.status(), body.reason());
    }

    @Operation(summary = "Feature or unfeature a business (admin only)")
    @PatchMapping("/{id}/featured")
    public AdminCompanyResponse setFeatured(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyFeaturedRequest body
    ) {
        return companies.setFeatured(me.getId(), id, body.featured());
    }
}

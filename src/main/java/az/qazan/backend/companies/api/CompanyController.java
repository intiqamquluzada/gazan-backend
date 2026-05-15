package az.qazan.backend.companies.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.companies.api.dto.CompanyResponse;
import az.qazan.backend.companies.api.dto.CreateCompanyRequest;
import az.qazan.backend.companies.api.dto.UpdateCompanyRequest;
import az.qazan.backend.companies.application.CompanyMapper;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Discover, manage businesses")
public class CompanyController {

    private final CompanyService companies;
    private final CompanyMapper mapper;
    private final UserService users;

    @Operation(summary = "List companies with optional category filter and search")
    @GetMapping
    public List<CompanyResponse> list(
            @Parameter(description = "Filter by category") @RequestParam(required = false) BusinessCategory category,
            @Parameter(description = "Search by name / tagline") @RequestParam(required = false) String q
    ) {
        return companies.search(category, q).stream().map(mapper::toResponse).toList();
    }

    @Operation(summary = "Companies marked as featured for the discover banner")
    @GetMapping("/featured")
    public List<CompanyResponse> featured() {
        return companies.featured().stream().map(mapper::toResponse).toList();
    }

    @Operation(summary = "Get a single company by id")
    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable UUID id) {
        return mapper.toResponse(companies.getById(id));
    }

    @Operation(summary = "Get the company owned by the signed-in business user")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @GetMapping("/me")
    public CompanyResponse me(@CurrentUser AppUserPrincipal me) {
        return mapper.toResponse(companies.myCompany(me.getId()));
    }

    @Operation(summary = "Create the signed-in user's business profile")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody CreateCompanyRequest body
    ) {
        Company c = companies.create(users.getById(me.getId()), body);
        return mapper.toResponse(c);
    }

    @Operation(summary = "Update a company (owner only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CompanyResponse update(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest body
    ) {
        return mapper.toResponse(companies.update(id, me.getId(), body));
    }

    @Operation(summary = "Delete a company (owner only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        companies.delete(id, me.getId());
    }
}

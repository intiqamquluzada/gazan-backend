package az.qazan.backend.loyalty.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.loyalty.api.dto.CreateProgramRequest;
import az.qazan.backend.loyalty.api.dto.LoyaltyProgramResponse;
import az.qazan.backend.loyalty.api.dto.UpdateProgramRequest;
import az.qazan.backend.loyalty.application.LoyaltyMapper;
import az.qazan.backend.loyalty.application.LoyaltyProgramService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Loyalty programs")
public class LoyaltyProgramController {

    private final LoyaltyProgramService service;
    private final LoyaltyMapper mapper;

    @Operation(summary = "List programs for a company")
    @GetMapping("/companies/{companyId}/programs")
    public List<LoyaltyProgramResponse> listForCompany(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return service.listForCompany(companyId, activeOnly).stream()
                .map(mapper::toProgramResponse).toList();
    }

    @Operation(summary = "Get one program")
    @GetMapping("/programs/{id}")
    public LoyaltyProgramResponse get(@PathVariable UUID id) {
        return mapper.toProgramResponse(service.getById(id));
    }

    @Operation(summary = "Create program (business owner)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping("/companies/{companyId}/programs")
    @ResponseStatus(HttpStatus.CREATED)
    public LoyaltyProgramResponse create(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateProgramRequest body
    ) {
        return mapper.toProgramResponse(service.create(companyId, me.getId(), body));
    }

    @Operation(summary = "Update program (business owner)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PutMapping("/programs/{id}")
    public LoyaltyProgramResponse update(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProgramRequest body
    ) {
        return mapper.toProgramResponse(service.update(id, me.getId(), body));
    }

    @Operation(summary = "Delete program (business owner)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @DeleteMapping("/programs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        service.delete(id, me.getId());
    }
}

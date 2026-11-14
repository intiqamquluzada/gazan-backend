package az.qazan.backend.business.api;

import az.qazan.backend.business.api.dto.StaffRequest;
import az.qazan.backend.business.api.dto.StaffResponse;
import az.qazan.backend.business.application.BusinessStaffService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS_OWNER')")
@Tag(name = "Business")
@SecurityRequirement(name = "bearerAuth")
public class BusinessStaffController {

    private final BusinessStaffService staff;

    @Operation(summary = "My cashier accounts")
    @GetMapping
    public List<StaffResponse> list(@CurrentUser AppUserPrincipal me) {
        return staff.list(me.getId());
    }

    @Operation(summary = "Create a cashier account for my business")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@CurrentUser AppUserPrincipal me,
                                @Valid @RequestBody StaffRequest body) {
        return staff.create(me.getId(), body);
    }

    @Operation(summary = "Deactivate a cashier account")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        staff.deactivate(me.getId(), id);
    }
}

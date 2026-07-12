package az.qazan.backend.business.api;

import az.qazan.backend.business.api.dto.BusinessCustomerDetailResponse;
import az.qazan.backend.business.api.dto.BusinessCustomerResponse;
import az.qazan.backend.business.application.BusinessCustomerDetailService;
import az.qazan.backend.business.application.BusinessCustomersService;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Tag(name = "Business")
@SecurityRequirement(name = "bearerAuth")
public class BusinessCustomersController {

    private final BusinessCustomersService customers;
    private final BusinessCustomerDetailService customerDetail;

    @Operation(summary = "Customers who hold a loyalty card at the owner's business")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @GetMapping("/customers")
    public List<BusinessCustomerResponse> customers(
            @CurrentUser AppUserPrincipal me) {
        return customers.customersForOwner(me.getId());
    }

    @Operation(summary = "Rich detail for one customer at the owner's business")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @GetMapping("/customers/{userId}")
    public BusinessCustomerDetailResponse detail(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID userId) {
        return customerDetail.detail(me.getId(), userId);
    }
}

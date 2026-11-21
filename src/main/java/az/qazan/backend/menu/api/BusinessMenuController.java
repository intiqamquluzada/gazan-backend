package az.qazan.backend.menu.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.menu.api.dto.MenuItemRequest;
import az.qazan.backend.menu.api.dto.MenuItemResponse;
import az.qazan.backend.menu.application.MenuService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * A business owner manages the menu of THEIR OWN company. The company is
 * resolved from the caller, so an owner can never touch another venue's
 * items (update/delete re-check ownership of the item).
 */
@RestController
@RequestMapping("/api/v1/business/menu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
@Tag(name = "Menu")
@SecurityRequirement(name = "bearerAuth")
public class BusinessMenuController {

    private final MenuService service;
    private final CompanyService companies;

    @Operation(summary = "My company's menu")
    @GetMapping
    public List<MenuItemResponse> mine(@CurrentUser AppUserPrincipal me) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        return service.forCompany(companyId).stream()
                .map(MenuItemResponse::from).toList();
    }

    @Operation(summary = "Add an item to my menu")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@CurrentUser AppUserPrincipal me,
                                   @Valid @RequestBody MenuItemRequest body) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        return MenuItemResponse.from(service.create(companyId, body));
    }

    @Operation(summary = "Update an item on my menu")
    @PutMapping("/{id}")
    public MenuItemResponse update(@CurrentUser AppUserPrincipal me,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody MenuItemRequest body) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        return MenuItemResponse.from(service.updateOwned(id, companyId, body));
    }

    @Operation(summary = "Delete an item from my menu")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUserPrincipal me, @PathVariable UUID id) {
        UUID companyId = companies.myCompany(me.getId()).getId();
        service.deleteOwned(id, companyId);
    }
}

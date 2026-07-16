package az.qazan.backend.menu.api;

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
 * Structured menu. The list endpoint is public (customers view it);
 * mutations are ADMIN-only.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Menu")
public class MenuController {

    private final MenuService service;

    @Operation(summary = "Menu items for a company (public)")
    @GetMapping("/companies/{companyId}/menu")
    public List<MenuItemResponse> forCompany(@PathVariable UUID companyId) {
        return service.forCompany(companyId).stream()
                .map(MenuItemResponse::from).toList();
    }

    @Operation(summary = "Add a menu item (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/companies/{companyId}/menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(
            @PathVariable UUID companyId,
            @Valid @RequestBody MenuItemRequest body) {
        return MenuItemResponse.from(service.create(companyId, body));
    }

    @Operation(summary = "Update a menu item (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/menu/{id}")
    public MenuItemResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemRequest body) {
        return MenuItemResponse.from(service.update(id, body));
    }

    @Operation(summary = "Delete a menu item (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/menu/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

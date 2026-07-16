package az.qazan.backend.menu.api.dto;

import az.qazan.backend.menu.domain.MenuItem;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String category,
        String name,
        String description,
        BigDecimal price,
        int sortOrder
) {
    public static MenuItemResponse from(MenuItem m) {
        return new MenuItemResponse(
                m.getId(),
                m.getCategory(),
                m.getName(),
                m.getDescription(),
                m.getPrice(),
                m.getSortOrder()
        );
    }
}

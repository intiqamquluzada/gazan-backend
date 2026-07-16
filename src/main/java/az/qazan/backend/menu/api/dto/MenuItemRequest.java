package az.qazan.backend.menu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 400) String description,
        BigDecimal price,
        Integer sortOrder
) {
}

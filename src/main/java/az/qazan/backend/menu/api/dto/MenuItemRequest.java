package az.qazan.backend.menu.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 400) String description,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price,
        Integer sortOrder
) {
}

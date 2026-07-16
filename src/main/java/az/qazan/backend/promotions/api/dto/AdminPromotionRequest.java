package az.qazan.backend.promotions.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin create/update payload for a Discover promotion banner. Colours
 * are unsigned 32-bit ARGB values (e.g. 0xFF6C2BD9). {@code companyId}
 * is optional — a promotion can be global or tied to one business.
 */
public record AdminPromotionRequest(
        UUID companyId,
        @NotBlank @Size(max = 40) String tag,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 200) String subtitle,
        @Size(max = 8) String emoji,
        Long gradientStartHex,
        Long gradientEndHex,
        @Size(max = 40) String cta,
        Boolean active,
        Instant endsAt,
        Integer sortOrder
) {
}

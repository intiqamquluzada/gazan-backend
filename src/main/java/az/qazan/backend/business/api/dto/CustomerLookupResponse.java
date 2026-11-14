package az.qazan.backend.business.api.dto;

import java.util.UUID;

public record CustomerLookupResponse(UUID userId, String fullName, String phone) {
}

package az.qazan.backend.media.api.dto;

import java.util.UUID;

/**
 * {@code url} is a relative path; the client prefixes it with the API
 * base URL before persisting it on the company profile.
 */
public record ImageUploadResponse(UUID id, String url) {
}

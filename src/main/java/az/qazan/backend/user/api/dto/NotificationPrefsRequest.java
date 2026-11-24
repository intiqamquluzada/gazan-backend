package az.qazan.backend.user.api.dto;

/** Push categories; a null field keeps its current value. */
public record NotificationPrefsRequest(
        Boolean rewards,
        Boolean offers,
        Boolean weekly,
        Boolean marketing
) {
}

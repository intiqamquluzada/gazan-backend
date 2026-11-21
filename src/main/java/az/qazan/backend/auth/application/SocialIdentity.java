package az.qazan.backend.auth.application;

/** What a verified Google or Apple ID token says about the user. */
public record SocialIdentity(String subject, String email, boolean emailVerified, String name) {
}

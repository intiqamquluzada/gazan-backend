package az.qazan.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Every error the API produces is identified by a stable code.
 * Mobile clients switch on the code; the human message is localized.
 */
@Getter
public enum ErrorCode {

    // ── Generic ────────────────────────────────────────────────
    VALIDATION_FAILED("error.validation", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("error.bad_request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("error.not_found", HttpStatus.NOT_FOUND),
    CONFLICT("error.conflict", HttpStatus.CONFLICT),
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN),
    INTERNAL("error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    RATE_LIMITED("error.rate_limited", HttpStatus.TOO_MANY_REQUESTS),
    PAYLOAD_TOO_LARGE("error.payload_too_large", HttpStatus.PAYLOAD_TOO_LARGE),

    // ── Auth ───────────────────────────────────────────────────
    AUTH_INVALID_CREDENTIALS("error.auth.invalid_credentials", HttpStatus.UNAUTHORIZED),
    AUTH_EMAIL_TAKEN("error.auth.email_taken", HttpStatus.CONFLICT),
    AUTH_TOKEN_EXPIRED("error.auth.token_expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("error.auth.token_invalid", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_INVALID("error.auth.refresh_invalid", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("error.auth.account_disabled", HttpStatus.FORBIDDEN),
    AUTH_RESET_CODE_INVALID("error.auth.reset_code_invalid", HttpStatus.BAD_REQUEST),
    AUTH_RESET_LOCKED("error.auth.reset_locked", HttpStatus.TOO_MANY_REQUESTS),
    AUTH_PHONE_TAKEN("error.auth.phone_taken", HttpStatus.CONFLICT),
    AUTH_SOCIAL_NOT_CONFIGURED("error.auth.social_not_configured", HttpStatus.SERVICE_UNAVAILABLE),
    AUTH_SOCIAL_INVALID("error.auth.social_invalid", HttpStatus.UNAUTHORIZED),

    // ── User ───────────────────────────────────────────────────
    USER_NOT_FOUND("error.user.not_found", HttpStatus.NOT_FOUND),
    AUTH_EMAIL_NOT_REGISTERED("error.auth.email_not_registered", HttpStatus.NOT_FOUND),
    AUTH_EMAIL_NOT_VERIFIED("error.auth.email_not_verified", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_MISMATCH("error.user.password_mismatch", HttpStatus.BAD_REQUEST),

    // Reviews
    REVIEW_NOT_ELIGIBLE("error.review.not_eligible", HttpStatus.FORBIDDEN),
    REVIEW_OWN_BUSINESS("error.review.own_business", HttpStatus.FORBIDDEN),
    REVIEW_REPORT_OWN("error.review.report_own", HttpStatus.BAD_REQUEST),

    // NFC cards
    NFC_UNKNOWN_TAG("error.nfc.unknown_tag", HttpStatus.NOT_FOUND),
    NFC_TAG_REVOKED("error.nfc.tag_revoked", HttpStatus.FORBIDDEN),
    NFC_COOLDOWN("error.nfc.cooldown", HttpStatus.TOO_MANY_REQUESTS),
    NFC_DAILY_CAP("error.nfc.daily_cap", HttpStatus.TOO_MANY_REQUESTS),
    NFC_OUT_OF_RANGE("error.nfc.out_of_range", HttpStatus.FORBIDDEN),
    NFC_MODE_OFF("error.nfc.mode_off", HttpStatus.FORBIDDEN),
    NFC_REPLAY("error.nfc.replay", HttpStatus.FORBIDDEN),
    NFC_TAG_UNASSIGNED("error.nfc.tag_unassigned", HttpStatus.BAD_REQUEST),
    NFC_NO_REWARDS("error.nfc.no_rewards", HttpStatus.BAD_REQUEST),
    NFC_MISSING_UID("error.nfc.missing_uid", HttpStatus.BAD_REQUEST),

    // Companies and coins
    COMPANY_NOT_APPROVED("error.company.not_approved", HttpStatus.FORBIDDEN),
    BUSINESS_NOT_SET_UP("error.business.not_set_up", HttpStatus.NOT_FOUND),
    COIN_INSUFFICIENT("error.coin.insufficient", HttpStatus.BAD_REQUEST),
    GRANT_CAP_EXCEEDED("error.coin.grant_cap", HttpStatus.BAD_REQUEST),
    // Campaigns and gifts
    PROGRAM_HAS_PENDING_REWARDS("error.program.has_pending_rewards", HttpStatus.CONFLICT),
    PROGRAM_CLOSED("error.program.closed", HttpStatus.BAD_REQUEST),
    VOUCHER_ALREADY_USED("error.voucher.already_used", HttpStatus.CONFLICT),
    SPEND_SESSION_EXPIRED("error.spend.session_expired", HttpStatus.BAD_REQUEST),
    REWARD_INACTIVE("error.reward.inactive", HttpStatus.BAD_REQUEST),
    NFC_TAP_TOO_OLD("error.nfc.tap_too_old", HttpStatus.BAD_REQUEST);

    private final String messageKey;
    private final HttpStatus status;

    ErrorCode(String messageKey, HttpStatus status) {
        this.messageKey = messageKey;
        this.status = status;
    }
}

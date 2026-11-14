package az.qazan.backend.user.application;

/** Stored phone format: a plus sign followed by digits only. */
public final class PhoneNumbers {

    private PhoneNumbers() {}

    /** @return the normalized number, or null when it holds no digits. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        return digits.isEmpty() ? null : "+" + digits;
    }
}

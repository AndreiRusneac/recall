package app.recall.core;

/** Turns phone numbers as people type them into the digits-only international form. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * Examples with default country code "40":
     * "0745 123 456" becomes "40745123456", "+40 745 123 456" becomes "40745123456".
     */
    public static String toInternational(String raw, String defaultCountryCode) {
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (trimmed.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("00")) {
            return digits.substring(2);
        }
        if (digits.startsWith("0")) {
            return defaultCountryCode + digits.substring(1);
        }
        return digits;
    }
}

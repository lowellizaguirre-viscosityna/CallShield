package com.lowell.callshield.screening;

public final class PhoneNumberClassifier {
    private PhoneNumberClassifier() {}

    public static boolean isForeign(String number, String homeCallingCode) {
        if (number == null || number.isBlank()) {
            return false;
        }
        String normalized = number.replaceAll("[\\s().-]", "");
        if (!normalized.startsWith("+")) {
            return false;
        }
        return !normalized.startsWith(homeCallingCode);
    }
}

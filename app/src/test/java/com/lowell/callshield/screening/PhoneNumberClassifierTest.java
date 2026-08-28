package com.lowell.callshield.screening;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PhoneNumberClassifierTest {
    @Test
    public void detectsForeignInternationalNumber() {
        assertTrue(PhoneNumberClassifier.isForeign("+44 20 7946 0958", "+1"));
    }

    @Test
    public void recognizesHomeCallingCode() {
        assertFalse(PhoneNumberClassifier.isForeign("+1 (212) 555-0100", "+1"));
    }

    @Test
    public void treatsNationalAndMissingNumbersAsUnknownNotForeign() {
        assertFalse(PhoneNumberClassifier.isForeign("2125550100", "+1"));
        assertFalse(PhoneNumberClassifier.isForeign(null, "+1"));
    }
}

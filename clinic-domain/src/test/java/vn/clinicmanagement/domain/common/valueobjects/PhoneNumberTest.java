package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberTest {

    @Test
    void acceptsSupportedVietnameseMobilePrefixesWithZeroOrCountryCode() {
        for (char prefix : new char[]{'3', '5', '7', '8', '9'}) {
            assertEquals("0" + prefix + "12345678", new PhoneNumber("0" + prefix + "12345678").getValue());
            assertEquals("+84" + prefix + "12345678", new PhoneNumber("+84" + prefix + "12345678").getValue());
        }
    }

    @Test
    void rejectsPipeAsMobilePrefix() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("0|12345678"));
    }

    @Test
    void rejectsUnsupportedPrefixAndInvalidLengths() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("0123456789"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("0212345678"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("031234567"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("03123456789"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("+84212345678"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("+8431234567"));
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(null));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(""));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("   "));
    }

    @Test
    void removesWhitespaceDotsAndHyphensBeforeValidation() {
        assertEquals("0312345678", new PhoneNumber(" 03 123.456-78 ").getValue());
        assertEquals("+84312345678", new PhoneNumber("+84 3.123-456 78").getValue());
    }
}

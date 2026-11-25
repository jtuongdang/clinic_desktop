package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DienThoaiSoTest {

    @Test
    void acceptsSupportedVietnameseMobilePrefixesWithZeroOrCountryCode() {
        for (char prefix : new char[]{'3', '5', '7', '8', '9'}) {
            assertEquals("0" + prefix + "12345678", new DienThoaiSo("0" + prefix + "12345678").getValue());
            assertEquals("+84" + prefix + "12345678", new DienThoaiSo("+84" + prefix + "12345678").getValue());
        }
    }

    @Test
    void rejectsPipeAsMobilePrefix() {
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("0|12345678"));
    }

    @Test
    void rejectsUnsupportedPrefixAndInvalidLengths() {
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("0123456789"));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("0212345678"));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("031234567"));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("03123456789"));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("+84212345678"));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("+8431234567"));
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo(null));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo(""));
        assertThrows(IllegalArgumentException.class, () -> new DienThoaiSo("   "));
    }

    @Test
    void invalidInputExceptionMessagesDoNotExposeSuppliedPhoneValues() {
        for (String rawPhone : new String[]{"synthetic-private-phone-marker", " 02 123.456-78 "}) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new DienThoaiSo(rawPhone));

            assertEquals("Số điện thoại không hợp lệ.", exception.getMessage());
            assertFalse(exception.getMessage().contains(rawPhone));
            assertFalse(exception.getMessage().contains(rawPhone.replaceAll("[\\s.-]", "")));
        }
    }

    @Test
    void removesWhitespaceDotsAndHyphensBeforeValidation() {
        assertEquals("0312345678", new DienThoaiSo(" 03 123.456-78 ").getValue());
        assertEquals("+84312345678", new DienThoaiSo("+84 3.123-456 78").getValue());
    }
}

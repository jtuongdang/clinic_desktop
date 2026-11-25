package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TienTest {

    @Test
    void defaultsToVndAndRoundsToTwoDecimalPlacesHalfUp() {
        assertEquals(Tien.CURRENCY_VND, new Tien(new BigDecimal("125.00")).getCurrency());
        assertEquals(new Tien(new BigDecimal("1.005")), new Tien(new BigDecimal("1.005"), null));
        assertEquals(new BigDecimal("1.01"), new Tien(new BigDecimal("1.005")).getAmount());
        assertEquals(new BigDecimal("1.00"), new Tien(new BigDecimal("1.004")).getAmount());
        assertEquals(new BigDecimal("-1.01"), new Tien(new BigDecimal("-1.005")).getAmount());
        assertEquals(new BigDecimal("0.00"), Tien.ZERO.getAmount());
    }

    @Test
    void factoriesCreateCanonicalVndWithTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.00"), Tien.of(12L).getAmount());
        assertEquals(new BigDecimal("1.01"), Tien.of(1.005).getAmount());
        assertEquals(Tien.CURRENCY_VND, Tien.of(12L).getCurrency());
        assertEquals(Tien.CURRENCY_VND, Tien.of(1.005).getCurrency());
    }

    @Test
    void addsSubtractsAndMultipliesAmountsWhilePreservingCurrency() {
        Tien first = new Tien(new BigDecimal("100.25"), "vNd");
        Tien second = new Tien(new BigDecimal("20.10"), null);

        assertEquals(new Tien(new BigDecimal("120.35")), first.add(second));
        assertEquals(new Tien(new BigDecimal("80.15")), first.subtract(second));
        assertEquals(new Tien(new BigDecimal("200.50")),
                first.multiply(new BigDecimal("2")));
        assertEquals(new Tien(new BigDecimal("0.33")),
                new Tien(new BigDecimal("1.00")).multiply(new BigDecimal("0.33")));
    }

    @Test
    void arithmeticPreservesTwoDecimalPlacesAndHalfUpRounding() {
        Tien rounded = new Tien(new BigDecimal("1.005"));

        assertEquals(new BigDecimal("2.02"), rounded.add(rounded).getAmount());
        assertEquals(new BigDecimal("0.00"), rounded.subtract(rounded).getAmount());
        assertEquals(new BigDecimal("0.51"), rounded.multiply(new BigDecimal("0.5")).getAmount());
        assertEquals(new BigDecimal("-0.51"), rounded.multiply(new BigDecimal("-0.5")).getAmount());
    }

    @Test
    void comparesAmountsInTheSameCurrency() {
        Tien lower = Tien.of(10);
        Tien higher = Tien.of(20);

        assertTrue(higher.isGreaterThan(lower));
        assertTrue(lower.isLessThan(higher));
        assertEquals(0, lower.compareTo(new Tien(new BigDecimal("10.00"))));
        assertTrue(Tien.ZERO.isZeroOrNegative());
        assertTrue(Tien.of(-1).isZeroOrNegative());
        assertFalse(higher.isZeroOrNegative());
    }

    @Test
    void canonicalizesVndCaseVariantsForComparisonEqualityAndHashing() {
        Tien expected = Tien.of(10);

        for (String currency : new String[]{"VND", "VNd", "VnD", "Vnd", "vND", "vNd", "vnD", "vnd"}) {
            Tien actual = new Tien(new BigDecimal("10.00"), currency);

            assertEquals(Tien.CURRENCY_VND, actual.getCurrency());
            assertEquals(0, actual.compareTo(expected));
            assertEquals(expected, actual);
            assertEquals(expected.hashCode(), actual.hashCode());
        }
    }

    @Test
    void rejectsExplicitUnsupportedCurrencyCodesAtConstruction() {
        for (String currency : new String[]{"USD", "usd", "EUR", "eur", "JPY", "XYZ", "", " ", " VND", "VND "}) {
            assertThrows(IllegalArgumentException.class, () -> new Tien(BigDecimal.ONE, currency), currency);
        }
    }

    @Test
    void rejectsNullAmountsFactorsAndOperands() {
        assertThrows(IllegalArgumentException.class, () -> new Tien(null));
        assertThrows(IllegalArgumentException.class, () -> new Tien(null, "VND"));
        assertThrows(IllegalArgumentException.class, () -> new Tien(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Tien.of(1).multiply(null));
        assertThrows(IllegalArgumentException.class, () -> Tien.of(1).add(null));
        assertThrows(IllegalArgumentException.class, () -> Tien.of(1).subtract(null));
        assertThrows(IllegalArgumentException.class, () -> Tien.of(1).compareTo(null));
        assertThrows(IllegalArgumentException.class, () -> Tien.of(1).isGreaterThan(null));
        assertThrows(IllegalArgumentException.class, () -> Tien.of(1).isLessThan(null));
    }

    @Test
    void equalValuesHaveEqualHashesAndDifferentValuesAreNotEqual() {
        Tien first = new Tien(new BigDecimal("10.0"), "VND");
        Tien equivalent = new Tien(new BigDecimal("10.00"), "VND");

        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertNotEquals(first, new Tien(new BigDecimal("10.01"), "VND"));
        assertNotEquals(first, null);
        assertNotEquals(first, "10.00 VND");
    }
}

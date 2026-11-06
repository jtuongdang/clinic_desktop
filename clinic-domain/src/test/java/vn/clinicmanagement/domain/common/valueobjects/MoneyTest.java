package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void defaultsToVndAndRoundsToTwoDecimalPlacesHalfUp() {
        assertEquals(Money.CURRENCY_VND, new Money(new BigDecimal("125.00")).getCurrency());
        assertEquals(new Money(new BigDecimal("1.005")), new Money(new BigDecimal("1.005"), null));
        assertEquals(new BigDecimal("1.01"), new Money(new BigDecimal("1.005")).getAmount());
        assertEquals(new BigDecimal("1.00"), new Money(new BigDecimal("1.004")).getAmount());
        assertEquals(new BigDecimal("-1.01"), new Money(new BigDecimal("-1.005")).getAmount());
        assertEquals(new BigDecimal("0.00"), Money.ZERO.getAmount());
    }

    @Test
    void factoriesCreateCanonicalVndWithTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.00"), Money.of(12L).getAmount());
        assertEquals(new BigDecimal("1.01"), Money.of(1.005).getAmount());
        assertEquals(Money.CURRENCY_VND, Money.of(12L).getCurrency());
        assertEquals(Money.CURRENCY_VND, Money.of(1.005).getCurrency());
    }

    @Test
    void addsSubtractsAndMultipliesAmountsWhilePreservingCurrency() {
        Money first = new Money(new BigDecimal("100.25"), "vNd");
        Money second = new Money(new BigDecimal("20.10"), null);

        assertEquals(new Money(new BigDecimal("120.35")), first.add(second));
        assertEquals(new Money(new BigDecimal("80.15")), first.subtract(second));
        assertEquals(new Money(new BigDecimal("200.50")),
                first.multiply(new BigDecimal("2")));
        assertEquals(new Money(new BigDecimal("0.33")),
                new Money(new BigDecimal("1.00")).multiply(new BigDecimal("0.33")));
    }

    @Test
    void arithmeticPreservesTwoDecimalPlacesAndHalfUpRounding() {
        Money rounded = new Money(new BigDecimal("1.005"));

        assertEquals(new BigDecimal("2.02"), rounded.add(rounded).getAmount());
        assertEquals(new BigDecimal("0.00"), rounded.subtract(rounded).getAmount());
        assertEquals(new BigDecimal("0.51"), rounded.multiply(new BigDecimal("0.5")).getAmount());
        assertEquals(new BigDecimal("-0.51"), rounded.multiply(new BigDecimal("-0.5")).getAmount());
    }

    @Test
    void comparesAmountsInTheSameCurrency() {
        Money lower = Money.of(10);
        Money higher = Money.of(20);

        assertTrue(higher.isGreaterThan(lower));
        assertTrue(lower.isLessThan(higher));
        assertEquals(0, lower.compareTo(new Money(new BigDecimal("10.00"))));
        assertTrue(Money.ZERO.isZeroOrNegative());
        assertTrue(Money.of(-1).isZeroOrNegative());
        assertFalse(higher.isZeroOrNegative());
    }

    @Test
    void canonicalizesVndCaseVariantsForComparisonEqualityAndHashing() {
        Money expected = Money.of(10);

        for (String currency : new String[]{"VND", "VNd", "VnD", "Vnd", "vND", "vNd", "vnD", "vnd"}) {
            Money actual = new Money(new BigDecimal("10.00"), currency);

            assertEquals(Money.CURRENCY_VND, actual.getCurrency());
            assertEquals(0, actual.compareTo(expected));
            assertEquals(expected, actual);
            assertEquals(expected.hashCode(), actual.hashCode());
        }
    }

    @Test
    void rejectsExplicitUnsupportedCurrencyCodesAtConstruction() {
        for (String currency : new String[]{"USD", "usd", "EUR", "eur", "JPY", "XYZ", "", " ", " VND", "VND "}) {
            assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.ONE, currency), currency);
        }
    }

    @Test
    void rejectsNullAmountsFactorsAndOperands() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null));
        assertThrows(IllegalArgumentException.class, () -> new Money(null, "VND"));
        assertThrows(IllegalArgumentException.class, () -> new Money(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(1).multiply(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).add(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).subtract(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).compareTo(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).isGreaterThan(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).isLessThan(null));
    }

    @Test
    void equalValuesHaveEqualHashesAndDifferentValuesAreNotEqual() {
        Money first = new Money(new BigDecimal("10.0"), "VND");
        Money equivalent = new Money(new BigDecimal("10.00"), "VND");

        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertNotEquals(first, new Money(new BigDecimal("10.01"), "VND"));
        assertNotEquals(first, null);
        assertNotEquals(first, "10.00 VND");
    }
}

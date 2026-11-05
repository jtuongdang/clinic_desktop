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
        assertEquals(Money.CURRENCY_VND, new Money(new BigDecimal("1.005"), null).getCurrency());
        assertEquals(new BigDecimal("1.01"), new Money(new BigDecimal("1.005")).getAmount());
        assertEquals(new BigDecimal("1.00"), new Money(new BigDecimal("1.004")).getAmount());
    }

    @Test
    void addsSubtractsAndMultipliesAmountsWhilePreservingCurrency() {
        Money first = new Money(new BigDecimal("100.25"), "USD");
        Money second = new Money(new BigDecimal("20.10"), "USD");

        assertEquals(new Money(new BigDecimal("120.35"), "USD"), first.add(second));
        assertEquals(new Money(new BigDecimal("80.15"), "USD"), first.subtract(second));
        assertEquals(new Money(new BigDecimal("200.50"), "USD"),
                first.multiply(new BigDecimal("2")));
        assertEquals(new Money(new BigDecimal("0.33"), "USD"),
                new Money(new BigDecimal("1.00"), "USD").multiply(new BigDecimal("0.33")));
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
    void rejectsArithmeticAndComparisonAcrossDifferentCurrencies() {
        Money usd = new Money(BigDecimal.ONE, "USD");
        Money eur = new Money(BigDecimal.ONE, "EUR");

        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.compareTo(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.isGreaterThan(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.isLessThan(eur));
    }

    @Test
    void rejectsNullAmountsFactorsAndOperands() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null));
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(1).multiply(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).add(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).subtract(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).compareTo(null));
    }

    @Test
    void equalValuesHaveEqualHashesAndDifferentValuesAreNotEqual() {
        Money first = new Money(new BigDecimal("10.0"), "VND");
        Money equivalent = new Money(new BigDecimal("10.00"), "VND");

        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertNotEquals(first, new Money(new BigDecimal("10.01"), "VND"));
        assertNotEquals(first, new Money(new BigDecimal("10.00"), "USD"));
        assertNotEquals(first, null);
        assertNotEquals(first, "10.00 VND");
    }
}

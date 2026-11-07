package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money implements Serializable, Comparable<Money> {
    public static final String CURRENCY_VND = "VND";
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount) {
        this(amount, CURRENCY_VND);
    }

    public Money(BigDecimal amount, String currency) {
        if (amount == null) throw new IllegalArgumentException("Số tiền không được null.");
        if (currency != null && !CURRENCY_VND.equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tiền tệ VND.");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = CURRENCY_VND;
    }

    public static Money of(long amount) { return new Money(BigDecimal.valueOf(amount)); }
    public static Money of(double amount) { return new Money(BigDecimal.valueOf(amount)); }

    public Money add(Money other) {
        checkCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        checkCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) throw new IllegalArgumentException("Hệ số nhân không được null.");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isGreaterThan(Money other) { return compareTo(other) > 0; }
    public boolean isLessThan(Money other) { return compareTo(other) < 0; }
    public boolean isZeroOrNegative() { return this.amount.compareTo(BigDecimal.ZERO) <= 0; }

    private void checkCurrency(Money other) {
        if (other == null || !this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException("Không thể tính toán giữa hai loại tiền tệ khác nhau.");
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    @Override
    public int compareTo(Money o) {
        checkCurrency(o);
        return this.amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, currency); }

    @Override
    public String toString() { return amount.toPlainString() + " " + currency; }
}

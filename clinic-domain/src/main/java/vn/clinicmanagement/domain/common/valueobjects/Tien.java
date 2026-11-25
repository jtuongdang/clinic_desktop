package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Tien implements Serializable, Comparable<Tien> {
    public static final String CURRENCY_VND = "VND";
    public static final Tien ZERO = new Tien(BigDecimal.ZERO);

    private final BigDecimal amount;
    private final String currency;

    public Tien(BigDecimal amount) {
        this(amount, CURRENCY_VND);
    }

    public Tien(BigDecimal amount, String currency) {
        if (amount == null) throw new IllegalArgumentException("Số tiền không được null.");
        if (currency != null && !CURRENCY_VND.equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tiền tệ VND.");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = CURRENCY_VND;
    }

    public static Tien of(long amount) { return new Tien(BigDecimal.valueOf(amount)); }
    public static Tien of(double amount) { return new Tien(BigDecimal.valueOf(amount)); }

    public Tien add(Tien other) {
        checkCurrency(other);
        return new Tien(this.amount.add(other.amount), this.currency);
    }

    public Tien subtract(Tien other) {
        checkCurrency(other);
        return new Tien(this.amount.subtract(other.amount), this.currency);
    }

    public Tien multiply(BigDecimal factor) {
        if (factor == null) throw new IllegalArgumentException("Hệ số nhân không được null.");
        return new Tien(this.amount.multiply(factor), this.currency);
    }

    public boolean isGreaterThan(Tien other) { return compareTo(other) > 0; }
    public boolean isLessThan(Tien other) { return compareTo(other) < 0; }
    public boolean isZeroOrNegative() { return this.amount.compareTo(BigDecimal.ZERO) <= 0; }

    private void checkCurrency(Tien other) {
        if (other == null || !this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException("Không thể tính toán giữa hai loại tiền tệ khác nhau.");
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    @Override
    public int compareTo(Tien o) {
        checkCurrency(o);
        return this.amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tien money = (Tien) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, currency); }

    @Override
    public String toString() { return amount.toPlainString() + " " + currency; }
}

package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public final class PhoneNumber implements Serializable {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[35789][0-9]{8}$");
    private final String value;

    public PhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) throw new IllegalArgumentException("Số điện thoại không được trống.");
        String cleaned = rawPhone.replaceAll("[\\s.-]", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) throw new IllegalArgumentException("Số điện thoại không hợp lệ: " + rawPhone);
        this.value = cleaned;
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}

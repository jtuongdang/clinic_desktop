package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DienThoaiSo implements Serializable {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[35789][0-9]{8}$");
    /** Trường `value`: giá trị. */
    private final String value;

    public DienThoaiSo(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) throw new IllegalArgumentException("Số điện thoại không được trống.");
        String cleaned = rawPhone.replaceAll("[\\s.-]", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        this.value = cleaned;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DienThoaiSo that = (DienThoaiSo) o;
        return Objects.equals(value, that.value);
    }

    /** Hàm `hashCode`: `hash` mã. Gốc khai báo tại `Serializable#hashCode`. */
    @Override
    public int hashCode() { return Objects.hash(value); }

    /** Hàm `toString`: den `String`. Gốc khai báo tại `Serializable#toString`. */
    @Override
    public String toString() { return value; }
}

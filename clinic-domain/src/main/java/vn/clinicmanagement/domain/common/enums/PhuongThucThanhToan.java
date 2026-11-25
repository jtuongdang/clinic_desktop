package vn.clinicmanagement.domain.common.enums;

public enum PhuongThucThanhToan {
    TIEN_MAT(1, "Tiền mặt"),
    CHUYEN_KHOAN(2, "Chuyển khoản"),
    THE(3, "Thẻ ngân hàng"),
    BAO_HIEM(4, "Bảo hiểm chi trả");

    private final int value;
    private final String description;

    PhuongThucThanhToan(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static PhuongThucThanhToan fromValue(int value) {
        for (PhuongThucThanhToan item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown PhuongThucThanhToan value: " + value);
    }
}

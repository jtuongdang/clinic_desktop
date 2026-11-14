package vn.clinicmanagement.domain.common.enums;

public enum PhuongThucThanhToan {
    TIEN_MAT(1, "Tiền mặt"),
    CHUYEN_KHOAN(2, "Chuyển khoản"),
    THE(3, "Thẻ ngân hàng"),
    BAO_HIEM(4, "Bảo hiểm chi trả");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    PhuongThucThanhToan(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static PhuongThucThanhToan fromValue(int value) {
        for (PhuongThucThanhToan item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown PhuongThucThanhToan value: " + value);
    }
}

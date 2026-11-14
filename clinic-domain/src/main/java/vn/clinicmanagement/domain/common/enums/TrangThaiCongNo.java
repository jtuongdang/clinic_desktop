package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiCongNo {
    DANG_NO(1, "Đang nợ"),
    DA_TAT_TOAN(2, "Đã tất toán"),
    QUA_HAN(3, "Quá hạn");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiCongNo(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiCongNo fromValue(int value) {
        for (TrangThaiCongNo item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiCongNo value: " + value);
    }
}

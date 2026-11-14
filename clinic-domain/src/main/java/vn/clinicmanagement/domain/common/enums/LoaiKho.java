package vn.clinicmanagement.domain.common.enums;

public enum LoaiKho {
    THUOC(1, "Kho thuốc"),
    VAT_TU(2, "Kho vật tư"),
    TONG_HOP(3, "Kho tổng hợp");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    LoaiKho(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static LoaiKho fromValue(int value) {
        for (LoaiKho item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiKho value: " + value);
    }
}

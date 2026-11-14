package vn.clinicmanagement.domain.common.enums;

public enum LoaiMatHang {
    THUOC(1, "Thuốc"),
    VAT_TU(2, "Vật tư y tế");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    LoaiMatHang(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static LoaiMatHang fromValue(int value) {
        for (LoaiMatHang item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiItem value: " + value);
    }
}

package vn.clinicmanagement.domain.common.enums;

public enum GioiTinh {
    M(1, "Nam"),
    F(2, "Nữ"),
    O(3, "Khác");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    GioiTinh(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static GioiTinh fromValue(int value) {
        for (GioiTinh item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown GioiTinh value: " + value);
    }
}

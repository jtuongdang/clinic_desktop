package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiLichLamViec {
    LAM_VIEC(1, "Làm việc"),
    NGHI(2, "Nghỉ"),
    HUY(3, "Hủy");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiLichLamViec(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiLichLamViec fromValue(int value) {
        for (TrangThaiLichLamViec item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiLichLamViec value: " + value);
    }
}

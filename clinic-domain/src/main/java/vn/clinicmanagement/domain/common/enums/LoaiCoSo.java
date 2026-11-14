package vn.clinicmanagement.domain.common.enums;

public enum LoaiCoSo {
    PHONG_KHAM_DA_KHOA(3, "Phòng khám đa khoa");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    LoaiCoSo(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static LoaiCoSo fromValue(int value) {
        for (LoaiCoSo item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiCoSo value: " + value);
    }
}

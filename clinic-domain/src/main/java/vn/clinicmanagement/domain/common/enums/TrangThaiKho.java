package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiKho {
    HOAT_DONG(1, "Hoạt động"),
    TAM_NGUNG(2, "Tạm ngưng"),
    DONG_CUA(3, "Đóng cửa");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiKho(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiKho fromValue(int value) {
        for (TrangThaiKho item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiKho value: " + value);
    }
}

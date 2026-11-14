package vn.clinicmanagement.domain.common.enums;

public enum LoaiTienSu {
    BENH_NEN(1, "Bệnh nền"),
    DI_UNG(2, "Dị ứng"),
    PHAU_THUAT(3, "Phẫu thuật"),
    TIEN_SU_GIA_DINH(4, "Tiền sử gia đình"),
    THOI_QUEN_SINH_HOAT(5, "Thói quen sinh hoạt");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    LoaiTienSu(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static LoaiTienSu fromValue(int value) {
        for (LoaiTienSu item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiTienSu value: " + value);
    }
}

package vn.clinicmanagement.domain.common.enums;

public enum HanhDongQuyen {
    XEM(1, "Xem"),
    TAO(2, "Tạo"),
    SUA(3, "Sửa"),
    XOA(4, "Xóa"),
    DUYET(5, "Duyệt"),
    IN(6, "In"),
    XUAT_FILE(7, "Xuất file"),
    MO_KHOA_EMR(8, "Mở khóa EMR"),
    AUDIT_READ(9, "Đọc nhật ký kiểm toán");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    HanhDongQuyen(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static HanhDongQuyen fromValue(int value) {
        for (HanhDongQuyen item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown HanhDongQuyen value: " + value);
    }
}

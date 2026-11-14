package vn.clinicmanagement.domain.common.enums;

public enum LoaiNghiPhep {
    PHEP_NAM(1, "Phép năm"),
    PHEP_BENH(2, "Phép bệnh"),
    PHEP_RIENG(3, "Phép riêng"),
    KHONG_LUONG(4, "Không lương");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    LoaiNghiPhep(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static LoaiNghiPhep fromValue(int value) {
        for (LoaiNghiPhep item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiNghiPhep value: " + value);
    }
}

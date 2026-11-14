package vn.clinicmanagement.domain.common.enums;

public enum PhamViDuLieu {
    TOAN_HE_THONG(1, "Toàn hệ thống"),
    CO_SO(2, "Cơ sở"),
    KHOA_PHONG(3, "Khoa phòng"),
    CHI_NHANH(4, "Chi nhánh");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    PhamViDuLieu(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static PhamViDuLieu fromValue(int value) {
        for (PhamViDuLieu item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown PhamViDuLieu value: " + value);
    }
}

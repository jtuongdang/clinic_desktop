package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiDieuChinhChamCong {
    CHO_DUYET(1, "Chờ duyệt"),
    DA_DUYET(2, "Đã duyệt"),
    TU_CHOI(3, "Từ chối");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiDieuChinhChamCong(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiDieuChinhChamCong fromValue(int value) {
        for (TrangThaiDieuChinhChamCong item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiDieuChinhChamCong value: " + value);
    }
}

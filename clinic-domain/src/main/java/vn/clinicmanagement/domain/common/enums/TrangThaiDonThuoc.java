package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiDonThuoc {
    TAM(1, "Lưu tạm"),
    DA_DUYET(2, "Đã duyệt"),
    DA_XUAT_HET(3, "Đã xuất hết"),
    HUY(4, "Đã hủy");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiDonThuoc(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiDonThuoc fromValue(int value) {
        for (TrangThaiDonThuoc item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiDonThuoc value: " + value);
    }
}

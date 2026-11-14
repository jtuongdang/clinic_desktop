package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiLuotKham {
    CHO_KHAM(1, "Chờ khám"),
    DANG_KHAM(2, "Đang khám"),
    DA_KHAM(3, "Đã khám"),
    KET_LUAN(4, "Kết luận"),
    HOAN_THANH(5, "Hoàn thành"),
    DA_HUY(6, "Đã hủy"),
    CHO_CLS(7, "Chờ cận lâm sàng");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiLuotKham(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiLuotKham fromValue(int value) {
        for (TrangThaiLuotKham item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiLuotKham value: " + value);
    }
}

package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiLuotKham {
    CHO_KHAM(1, "Chờ khám"),
    DANG_KHAM(2, "Đang khám"),
    DA_KHAM(3, "Đã khám"),
    KET_LUAN(4, "Kết luận"),
    HOAN_THANH(5, "Hoàn thành"),
    DA_HUY(6, "Đã hủy"),
    CHO_CLS(7, "Chờ cận lâm sàng");

    private final int value;
    private final String description;

    TrangThaiLuotKham(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiLuotKham fromValue(int value) {
        for (TrangThaiLuotKham item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiLuotKham value: " + value);
    }
}

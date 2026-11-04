package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiNhaCungCap {
    HOAT_DONG(1, "Đang hợp tác"),
    NGUNG_HOP_TAC(2, "Ngừng hợp tác");

    private final int value;
    private final String description;

    TrangThaiNhaCungCap(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiNhaCungCap fromValue(int value) {
        for (TrangThaiNhaCungCap item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiNhaCungCap value: " + value);
    }
}

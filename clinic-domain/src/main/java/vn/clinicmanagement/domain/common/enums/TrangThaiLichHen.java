package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiLichHen {
    CHO_XAC_NHAN(1, "Chờ xác nhận"),
    DA_XAC_NHAN(2, "Đã xác nhận"),
    DA_DEN(3, "Đã đến"),
    DA_HUY(4, "Đã hủy"),
    HOAN_THANH(5, "Hoàn thành");

    private final int value;
    private final String description;

    TrangThaiLichHen(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiLichHen fromValue(int value) {
        for (TrangThaiLichHen item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiLichHen value: " + value);
    }
}

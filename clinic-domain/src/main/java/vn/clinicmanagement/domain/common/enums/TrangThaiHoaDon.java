package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiHoaDon {
    CHO_THANH_TOAN(1, "Chờ thanh toán"),
    THANH_TOAN_MOT_PHAN(2, "Thanh toán một phần"),
    DA_THANH_TOAN(3, "Đã thanh toán"),
    DA_HUY(4, "Đã hủy");

    private final int value;
    private final String description;

    TrangThaiHoaDon(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiHoaDon fromValue(int value) {
        for (TrangThaiHoaDon item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiHoaDon value: " + value);
    }
}

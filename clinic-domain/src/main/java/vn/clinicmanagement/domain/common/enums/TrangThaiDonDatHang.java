package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiDonDatHang {
    CHO_DUYET(1, "Chờ duyệt"),
    DA_DAT(2, "Đã đặt hàng"),
    GIAO_MOT_PHAN(3, "Giao một phần"),
    HOAN_THANH(4, "Hoàn thành"),
    DA_HUY(5, "Đã hủy");

    private final int value;
    private final String description;

    TrangThaiDonDatHang(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiDonDatHang fromValue(int value) {
        for (TrangThaiDonDatHang item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiDonDatHang value: " + value);
    }
}

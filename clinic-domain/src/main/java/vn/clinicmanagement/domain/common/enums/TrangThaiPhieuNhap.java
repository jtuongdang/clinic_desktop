package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiPhieuNhap {
    TAM(1, "Lưu tạm"),
    CHO_DUYET(2, "Chờ duyệt"),
    DA_NHAP(3, "Đã nhập"),
    DA_HUY(4, "Đã hủy");

    private final int value;
    private final String description;

    TrangThaiPhieuNhap(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiPhieuNhap fromValue(int value) {
        for (TrangThaiPhieuNhap item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiPhieuNhap value: " + value);
    }
}

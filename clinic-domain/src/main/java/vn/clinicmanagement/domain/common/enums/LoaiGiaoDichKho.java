package vn.clinicmanagement.domain.common.enums;

public enum LoaiGiaoDichKho {
    NHAP(1, "Nhập kho"),
    XUAT_DON(2, "Xuất đơn thuốc"),
    XUAT_BAN_LE(3, "Xuất bán lẻ"),
    XUAT_THU_THUAT(4, "Xuất thủ thuật"),
    DIEU_CHINH(5, "Điều chỉnh"),
    HUY(6, "Hủy"),
    TRA_NCC(7, "Trả NCC"),
    NHAP_TRA_BENH_NHAN(8, "Bệnh nhân hoàn trả"),
    XUAT_CAP_PHAT_KHOA_PHONG(9, "Cấp phát khoa phòng");

    private final int value;
    private final String description;

    LoaiGiaoDichKho(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiGiaoDichKho fromValue(int value) {
        for (LoaiGiaoDichKho item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiGiaoDichKho value: " + value);
    }
}

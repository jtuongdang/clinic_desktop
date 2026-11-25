package vn.clinicmanagement.domain.common.enums;

public enum LoaiXuatKho {
    BAN_THUOC(1, "Bán thuốc"),
    XUAT_NOI_BO(2, "Xuất nội bộ"),
    HAO_HUT(3, "Hao hụt"),
    TRA_NCC(4, "Trả NCC"),
    CHUYEN_KHO(5, "Chuyển kho"),
    XUAT_DON(6, "Xuất theo đơn"),
    XUAT_THU_THUAT(7, "Xuất thủ thuật"),
    DIEU_CHINH(8, "Điều chỉnh"),
    HUY(9, "Hủy"),
    XUAT_NOI_BO_KHOA_PHONG(10, "Xuất nội bộ khoa phòng");

    private final int value;
    private final String description;

    LoaiXuatKho(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiXuatKho fromValue(int value) {
        for (LoaiXuatKho item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiXuatKho value: " + value);
    }
}

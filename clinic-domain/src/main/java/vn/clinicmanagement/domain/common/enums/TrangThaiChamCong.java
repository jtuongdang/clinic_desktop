package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiChamCong {
    CHUA_CHAM_CONG(1, "Chưa chấm công"),
    DI_DUNG_GIO(2, "Đúng giờ"),
    DI_MUON(3, "Đi muộn"),
    VE_SOM(4, "Về sớm"),
    DI_MUON_VE_SOM(5, "Đi muộn và về sớm"),
    NGHI_PHEP(6, "Nghỉ phép"),
    VANG_MAT(7, "Vắng mặt"),
    THIEU_CHECK_OUT(8, "Thiếu check-out"),
    CHO_XAC_NHAN(9, "Chờ xác nhận");

    private final int value;
    private final String description;

    TrangThaiChamCong(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiChamCong fromValue(int value) {
        for (TrangThaiChamCong item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiChamCong value: " + value);
    }
}

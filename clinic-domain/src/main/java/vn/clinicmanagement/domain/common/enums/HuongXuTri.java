package vn.clinicmanagement.domain.common.enums;

public enum HuongXuTri {
    CHO_VE_TOA(1, "Cho về toa / Ngoại trú"),
    HEN_TAI_KHAM(2, "Hẹn tái khám"),
    NHAP_VIEN_NOI_TRU(3, "Nhập viện nội trú"),
    CHUYEN_TUYEN_TREN(4, "Chuyển viện tuyến trên"),
    CHUYEN_CHUYEN_KHOA(5, "Chuyển chuyên khoa");

    private final int value;
    private final String description;

    HuongXuTri(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static HuongXuTri fromValue(int value) {
        for (HuongXuTri item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown HuongXuTri value: " + value);
    }
}

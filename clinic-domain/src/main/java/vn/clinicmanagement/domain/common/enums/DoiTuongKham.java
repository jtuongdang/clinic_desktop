package vn.clinicmanagement.domain.common.enums;

public enum DoiTuongKham {
    BHYT(1, "Bảo hiểm y tế"),
    THU_PHI(2, "Viện phí / Thu phí"),
    DICH_VU_YEU_CAU(3, "Dịch vụ theo yêu cầu"),
    MIEN_PHI(4, "Miễn phí"),
    UU_TIEN(5, "Ưu tiên (Trẻ em <6t, Người già >75t)");

    private final int value;
    private final String description;

    DoiTuongKham(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static DoiTuongKham fromValue(int value) {
        for (DoiTuongKham item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown DoiTuongKham value: " + value);
    }
}

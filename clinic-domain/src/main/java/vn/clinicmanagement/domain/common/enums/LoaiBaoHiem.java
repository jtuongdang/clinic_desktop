package vn.clinicmanagement.domain.common.enums;

public enum LoaiBaoHiem {
    BHYT(1, "Bảo hiểm y tế"),
    BAO_HIEM_SUC_KHOE_TU_NHAN(2, "Bảo hiểm tư nhân"),
    BAO_HIEM_TAI_NAN(3, "Bảo hiểm tai nạn"),
    KHAC(4, "Khác");

    private final int value;
    private final String description;

    LoaiBaoHiem(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiBaoHiem fromValue(int value) {
        for (LoaiBaoHiem item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiBaoHiem value: " + value);
    }
}

package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiKiemKe {
    MOI_TAO(1, "Mới tạo"),
    DA_NOP(2, "Đã nộp"),
    DA_DUYET(3, "Đã duyệt"),
    DA_HUY(4, "Đã hủy");

    private final int value;
    private final String description;

    TrangThaiKiemKe(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiKiemKe fromValue(int value) {
        for (TrangThaiKiemKe item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiKiemKe value: " + value);
    }
}

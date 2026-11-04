package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiNghiPhep {
    CHO_DUYET(1, "Chờ duyệt"),
    DA_DUYET(2, "Đã duyệt"),
    TU_CHOI(3, "Từ chối"),
    HUY(4, "Đã hủy");

    private final int value;
    private final String description;

    TrangThaiNghiPhep(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiNghiPhep fromValue(int value) {
        for (TrangThaiNghiPhep item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiNghiPhep value: " + value);
    }
}

package vn.clinicmanagement.domain.common.enums;

public enum LoaiCoSo {
    PHONG_KHAM_DA_KHOA(3, "Phòng khám đa khoa");

    private final int value;
    private final String description;

    LoaiCoSo(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiCoSo fromValue(int value) {
        for (LoaiCoSo item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiCoSo value: " + value);
    }
}

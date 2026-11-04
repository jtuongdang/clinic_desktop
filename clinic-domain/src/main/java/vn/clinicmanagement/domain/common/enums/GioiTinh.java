package vn.clinicmanagement.domain.common.enums;

public enum GioiTinh {
    M(1, "Nam"),
    F(2, "Nữ"),
    O(3, "Khác");

    private final int value;
    private final String description;

    GioiTinh(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static GioiTinh fromValue(int value) {
        for (GioiTinh item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown GioiTinh value: " + value);
    }
}

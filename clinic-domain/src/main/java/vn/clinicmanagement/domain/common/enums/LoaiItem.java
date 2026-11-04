package vn.clinicmanagement.domain.common.enums;

public enum LoaiItem {
    THUOC(1, "Thuốc"),
    VAT_TU(2, "Vật tư y tế");

    private final int value;
    private final String description;

    LoaiItem(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiItem fromValue(int value) {
        for (LoaiItem item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiItem value: " + value);
    }
}

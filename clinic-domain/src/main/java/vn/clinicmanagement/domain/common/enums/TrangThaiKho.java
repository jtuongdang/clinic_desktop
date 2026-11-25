package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiKho {
    HOAT_DONG(1, "Hoạt động"),
    TAM_NGUNG(2, "Tạm ngưng"),
    DONG_CUA(3, "Đóng cửa");

    private final int value;
    private final String description;

    TrangThaiKho(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiKho fromValue(int value) {
        for (TrangThaiKho item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiKho value: " + value);
    }
}

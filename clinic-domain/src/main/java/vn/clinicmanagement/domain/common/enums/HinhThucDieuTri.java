package vn.clinicmanagement.domain.common.enums;

public enum HinhThucDieuTri {
    NGOAI_TRU(1, "Ngoại trú"),
    NOI_TRU(2, "Nội trú"),
    CAP_CUU(3, "Cấp cứu");

    private final int value;
    private final String description;

    HinhThucDieuTri(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static HinhThucDieuTri fromValue(int value) {
        for (HinhThucDieuTri item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown HinhThucDieuTri value: " + value);
    }
}

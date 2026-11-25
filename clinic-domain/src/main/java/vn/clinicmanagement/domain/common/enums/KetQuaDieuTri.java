package vn.clinicmanagement.domain.common.enums;

public enum KetQuaDieuTri {
    KHOI(1, "Khỏi"),
    DO_GIAM(2, "Đỡ / Giảm"),
    KHONG_THAY_DOI(3, "Không thay đổi"),
    NANG_HON(4, "Nặng hơn"),
    TU_VONG(5, "Tử vong");

    private final int value;
    private final String description;

    KetQuaDieuTri(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static KetQuaDieuTri fromValue(int value) {
        for (KetQuaDieuTri item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown KetQuaDieuTri value: " + value);
    }
}

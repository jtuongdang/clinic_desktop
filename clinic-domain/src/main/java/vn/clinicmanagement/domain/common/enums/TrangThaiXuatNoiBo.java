package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiXuatNoiBo {
    CHO_DUYET(0, "Chờ duyệt"),
    DA_DUYET(1, "Đã duyệt"),
    DA_NHAN_HANG(2, "Đã nhận hàng"),
    TU_CHOI(3, "Từ chối");

    private final int value;
    private final String description;

    TrangThaiXuatNoiBo(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static TrangThaiXuatNoiBo fromValue(int value) {
        for (TrangThaiXuatNoiBo item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiXuatNoiBo value: " + value);
    }
}

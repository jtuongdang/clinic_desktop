package vn.clinicmanagement.domain.common.enums;

public enum NguonLich {
    TU_DONG_THEO_LICH_MAU(1, "Tự động theo lịch mẫu"),
    PHAT_SINH_THU_CONG(2, "Phát sinh thủ công"),
    DOI_CA(3, "Đổi ca");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    NguonLich(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static NguonLich fromValue(int value) {
        for (NguonLich item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown NguonLich value: " + value);
    }
}

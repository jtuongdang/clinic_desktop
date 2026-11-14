package vn.clinicmanagement.domain.common.enums;

public enum TrangThaiChiDinhDichVu {
    DA_CHI_DINH(1, "Đã chỉ định"),
    DA_TIEP_NHAN(2, "Đã tiếp nhận"),
    CHO_KET_QUA(3, "Chờ kết quả"),
    DA_CO_KET_QUA(4, "Đã có kết quả"),
    DA_HUY(5, "Đã hủy");

    /** Trường `value`: giá trị. */
    private final int value;
    private final String description;

    TrangThaiChiDinhDichVu(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /** Hàm `getValue`: lấy giá trị. */
    public int getValue() { return value; }
    /** Hàm `getDescription`: lấy `Description`. */
    public String getDescription() { return description; }

    /** Hàm `fromValue`: tu giá trị. */
    public static TrangThaiChiDinhDichVu fromValue(int value) {
        for (TrangThaiChiDinhDichVu item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiChiDinhDichVu value: " + value);
    }
}

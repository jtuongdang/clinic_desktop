package vn.clinicmanagement.domain.common.enums;

public enum LoaiDichVu {
    KHAM(1, "Khám bệnh"),
    XET_NGHIEM(2, "Xét nghiệm"),
    CHAN_DOAN_HINH_ANH(3, "Chẩn đoán hình ảnh"),
    DICH_VU_KY_THUAT(4, "Dịch vụ kỹ thuật");

    private final int value;
    private final String description;

    LoaiDichVu(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static LoaiDichVu fromValue(int value) {
        for (LoaiDichVu item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown LoaiDichVu value: " + value);
    }
}

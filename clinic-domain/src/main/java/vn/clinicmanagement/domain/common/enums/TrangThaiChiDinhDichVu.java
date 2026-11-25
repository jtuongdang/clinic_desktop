package vn.clinicmanagement.domain.common.enums;

import java.util.Locale;

public enum TrangThaiChiDinhDichVu {
    DA_CHI_DINH(1, "Đã chỉ định"),
    DA_TIEP_NHAN(2, "Đã tiếp nhận"),
    CHO_KET_QUA(3, "Chờ kết quả"),
    DA_CO_KET_QUA(4, "Đã trả kết quả"),
    DA_HUY(5, "Đã hủy"),
    DA_LAY_MAU(6, "Đã lấy mẫu"),
    CHO_THUC_HIEN(7, "Đang chờ"),
    DANG_THUC_HIEN(8, "Đang thực hiện"),
    DA_DUYET_KET_QUA(9, "Đã duyệt kết quả");

    private final int value;
    private final String description;

    TrangThaiChiDinhDichVu(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public boolean isWaitingForCollection() {
        return this == CHO_THUC_HIEN || this == DA_CHI_DINH;
    }

    public boolean isSampleCollectedWaitingExec() {
        return this == DA_LAY_MAU || this == DA_TIEP_NHAN || this == CHO_KET_QUA;
    }

    public boolean isExecuting() {
        return this == DANG_THUC_HIEN;
    }

    public boolean isApproved() {
        return this == DA_DUYET_KET_QUA;
    }

    public boolean isResultReleased() {
        return this == DA_CO_KET_QUA;
    }

    public boolean isCancelled() {
        return this == DA_HUY;
    }

    public boolean isVisibleInLabWorkspace() {
        return !isWaitingForCollection();
    }

    public static TrangThaiChiDinhDichVu fromCode(String code) {
        if (code == null || code.isBlank()) {
            return CHO_THUC_HIEN;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CHO_THUC_HIEN;
        }
    }

    public static TrangThaiChiDinhDichVu fromValue(int value) {
        for (TrangThaiChiDinhDichVu item : values()) {
            if (item.value == value) return item;
        }
        throw new IllegalArgumentException("Unknown TrangThaiChiDinhDichVu value: " + value);
    }
}

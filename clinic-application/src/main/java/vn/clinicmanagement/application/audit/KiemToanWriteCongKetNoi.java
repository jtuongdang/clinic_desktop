package vn.clinicmanagement.application.audit;

public interface KiemToanWriteCongKetNoi {
    /** Hàm `appendInCurrentTransaction`: thêm vào trong hientai giao dịch. */
    void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry);

    /** Hàm `appendIndependently`: thêm vào `Independently`. */
    void appendIndependently(HeThongKiemToanNhapLieu entry);

    /** Hàm `appendMedicalRecordInCurrentTransaction`: thêm vào yte ghi nhận trong hientai giao dịch. */
    void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry);

    /** Hàm `appendMedicalRecordIndependently`: thêm vào yte ghi nhận `Independently`. */
    void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry);
}

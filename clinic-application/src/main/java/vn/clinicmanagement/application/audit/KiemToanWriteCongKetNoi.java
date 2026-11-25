package vn.clinicmanagement.application.audit;

public interface KiemToanWriteCongKetNoi {
    void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry);

    void appendIndependently(HeThongKiemToanNhapLieu entry);

    void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry);

    void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry);
}

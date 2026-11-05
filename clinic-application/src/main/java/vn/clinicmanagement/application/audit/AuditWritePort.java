package vn.clinicmanagement.application.audit;

public interface AuditWritePort {
    void appendInCurrentTransaction(SystemAuditEntry entry);

    void appendIndependently(SystemAuditEntry entry);

    void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry);

    void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry);
}

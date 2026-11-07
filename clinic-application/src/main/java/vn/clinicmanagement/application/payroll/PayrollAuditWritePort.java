package vn.clinicmanagement.application.payroll;

public interface PayrollAuditWritePort {
    void appendIndependently(PayrollPeriodLockedAudit entry);
}

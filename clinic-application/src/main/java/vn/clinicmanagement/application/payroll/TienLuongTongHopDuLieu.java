package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;
import java.util.List;

public record TienLuongTongHopDuLieu(
        int periodId,
        String periodName,
        int month,
        int year,
        boolean locked,
        List<TienLuongDongDuLieu> payrollLines,
        BigDecimal totalNetSalary) {
    public TienLuongTongHopDuLieu {
        payrollLines = List.copyOf(payrollLines);
    }
}

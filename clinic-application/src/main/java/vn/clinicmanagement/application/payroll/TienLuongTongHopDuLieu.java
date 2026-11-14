package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;
import java.util.List;

public record TienLuongTongHopDuLieu(
        /** Trường `periodId`: ky mã. */
        int periodId,
        /** Trường `periodName`: ky tên. */
        String periodName,
        int month,
        int year,
        /** Trường `locked`: dakhoa. */
        boolean locked,
        /** Trường `payrollLines`: tienluong `Lines`. */
        List<TienLuongDongDuLieu> payrollLines,
        BigDecimal totalNetSalary) {
    public TienLuongTongHopDuLieu {
        payrollLines = List.copyOf(payrollLines);
    }
}

package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.domain.payroll.BangLuong;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.util.List;
import java.util.Optional;

/** Application boundary for payroll-period and payroll-statement persistence. */
public interface TienLuongRepository {
    /** Hàm `findPeriodById`: tìm ky theo mã. */
    Optional<KyLuong> findPeriodById(int periodId);

    /** Hàm `findPeriodByMonthAndYear`: tìm ky theo `Month` va `Year`. */
    Optional<KyLuong> findPeriodByMonthAndYear(int month, int year);

    /** Hàm `savePeriod`: lưu ky. */
    KyLuong savePeriod(KyLuong period);

    /** Hàm `savePayroll`: lưu tienluong. */
    BangLuong savePayroll(BangLuong payroll);

    /** Hàm `findPayrollByPeriodId`: tìm tienluong theo ky mã. */
    List<BangLuong> findPayrollByPeriodId(int periodId);
}

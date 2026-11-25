package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.domain.payroll.BangLuong;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.util.List;
import java.util.Optional;

/**
 * Cổng ứng dụng để lưu kỳ lương và bảng lương.
 */
public interface TienLuongRepository {
    Optional<KyLuong> findPeriodById(int periodId);

    Optional<KyLuong> findPeriodByMonthAndYear(int month, int year);

    KyLuong savePeriod(KyLuong period);

    BangLuong savePayroll(BangLuong payroll);

    List<BangLuong> findPayrollByPeriodId(int periodId);
}

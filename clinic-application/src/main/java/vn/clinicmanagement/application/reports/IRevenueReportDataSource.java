package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.domain.billing.HoaDon;

import java.time.LocalDate;
import java.util.List;

/**
 * Cổng giao tiếp nguồn dữ liệu báo cáo doanh thu (Application Port).
 * Cho phép linh hoạt nạp từ Domain Repository hoặc truy vấn tối ưu hạ tầng.
 */
@FunctionalInterface
public interface IRevenueReportDataSource {
    List<HoaDon> getInvoices(LocalDate fromDate, LocalDate toDate, Integer facilityId, Integer departmentId);
}

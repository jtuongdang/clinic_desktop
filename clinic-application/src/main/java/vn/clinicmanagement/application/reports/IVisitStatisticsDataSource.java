package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.domain.examination.LuotKham;

import java.time.LocalDate;
import java.util.List;

/**
 * Cổng giao tiếp nguồn dữ liệu lượt khám (Application Port).
 */
@FunctionalInterface
public interface IVisitStatisticsDataSource {
    List<LuotKham> getVisits(LocalDate fromDate, LocalDate toDate, Integer facilityId, Integer departmentId);
}

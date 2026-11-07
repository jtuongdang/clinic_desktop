package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.List;

public final class GetEmployeesHandler
        implements QueryHandler<GetEmployeesQuery, List<EmployeeSummaryDto>> {
    private final INhanVienRepository employees;

    public GetEmployeesHandler(INhanVienRepository employees) {
        this.employees = employees;
    }

    @Override
    public Result<List<EmployeeSummaryDto>> handle(GetEmployeesQuery query) {
        if (query == null) return Result.failure("HR_INVALID", "Bộ lọc nhân viên không hợp lệ.");
        List<EmployeeSummaryDto> summaries = employees.getByStatus(query.active()).stream()
                .map(employee -> new EmployeeSummaryDto(
                        employee.getId(), employee.getMaNhanVien(), employee.getHoTen(),
                        employee.getSoDienThoai(), employee.getEmail(), employee.getMaChucVu(),
                        employee.getUsername(), employee.isHoatDong()))
                .toList();
        return Result.success(summaries);
    }
}

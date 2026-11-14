package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.List;

public final class GetNhanVienHandler
        implements QueryHandler<GetNhanVienQuery, List<NhanVienTongHopDuLieu>> {
    /** Trường `employees`: nhân viên. */
    private final INhanVienRepository employees;

    public GetNhanVienHandler(INhanVienRepository employees) {
        this.employees = employees;
    }

    @Override
    public KetQua<List<NhanVienTongHopDuLieu>> handle(GetNhanVienQuery query) {
        if (query == null) return KetQua.failure("HR_INVALID", "Bộ lọc nhân viên không hợp lệ.");
        List<NhanVienTongHopDuLieu> summaries = employees.getByStatus(query.active()).stream()
                .map(employee -> new NhanVienTongHopDuLieu(
                        employee.getId(), employee.getMaNhanVien(), employee.getHoTen(),
                        employee.getSoDienThoai(), employee.getEmail(), employee.getMaChucVu(),
                        employee.getUsername(), employee.isHoatDong()))
                .toList();
        return KetQua.success(summaries);
    }
}

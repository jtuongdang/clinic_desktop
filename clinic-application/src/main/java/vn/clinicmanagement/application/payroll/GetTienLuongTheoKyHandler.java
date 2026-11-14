package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.payroll.BangLuong;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GetTienLuongTheoKyHandler
        implements QueryHandler<GetTienLuongTheoKyQuery, TienLuongTongHopDuLieu> {

    /** Trường `payrollRepository`: tienluong kho dữ liệu. */
    private final TienLuongRepository payrollRepository;

    public GetTienLuongTheoKyHandler(TienLuongRepository payrollRepository) {
        this.payrollRepository = Objects.requireNonNull(payrollRepository, "payrollRepository");
    }

    @Override
    public KetQua<TienLuongTongHopDuLieu> handle(GetTienLuongTheoKyQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Truy vấn bảng lương không được để trống.");
        }
        boolean byId = query.periodId() != null;
        boolean byMonthYear = query.month() != null && query.year() != null;
        if ((byId && (query.month() != null || query.year() != null))
                || (!byId && !byMonthYear)) {
            return KetQua.failure("INVALID_PAYROLL_PERIOD_QUERY",
                    "Cần cung cấp mã kỳ lương hoặc đồng thời tháng và năm.");
        }
        if (byId && query.periodId() <= 0) {
            return KetQua.failure("INVALID_PAYROLL_PERIOD", "Mã kỳ lương không hợp lệ.");
        }
        if (byMonthYear && (query.month() < 1 || query.month() > 12 || query.year() <= 0)) {
            return KetQua.failure("INVALID_PAYROLL_PERIOD", "Tháng hoặc năm không hợp lệ.");
        }

        Optional<KyLuong> periodOptional = byId
                ? payrollRepository.findPeriodById(query.periodId())
                : payrollRepository.findPeriodByMonthAndYear(query.month(), query.year());
        if (periodOptional.isEmpty()) {
            return KetQua.failure("PAYROLL_PERIOD_NOT_FOUND", "Không tìm thấy kỳ lương.");
        }
        KyLuong period = periodOptional.get();
        List<BangLuong> payrolls = payrollRepository.findPayrollByPeriodId(period.getId());
        if (payrolls == null) {
            return KetQua.failure("PAYROLL_READ_FAILED", "Không thể đọc bảng lương của kỳ.");
        }

        List<TienLuongDongDuLieu> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (BangLuong payroll : payrolls) {
            if (payroll == null) {
                return KetQua.failure("PAYROLL_READ_FAILED", "Bảng lương chứa dòng dữ liệu không hợp lệ.");
            }
            BigDecimal netSalary = payroll.getThucLinh();
            if (netSalary == null) {
                return KetQua.failure("PAYROLL_READ_FAILED", "Dòng bảng lương thiếu số tiền thực lĩnh.");
            }
            lines.add(new TienLuongDongDuLieu(
                    payroll.getId() != null ? payroll.getId() : 0,
                    payroll.getMaNhanVien(),
                    payroll.getLuongCoBan(),
                    payroll.getHeSoLuong(),
                    payroll.getTongPhuCap(),
                    payroll.getTongThuLaoKham(),
                    payroll.getTongKhauTru(),
                    netSalary));
            total = total.add(netSalary);
        }
        return KetQua.success(new TienLuongTongHopDuLieu(
                period.getId(),
                period.getTenKy(),
                period.getThang(),
                period.getNam(),
                period.isDaChot(),
                lines,
                total));
    }
}

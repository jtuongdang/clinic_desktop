package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.payroll.BangLuong;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.math.BigDecimal;
import java.util.Objects;

public final class CalculatePayrollHandler
        implements CommandHandler<CalculatePayrollCommand, CalculatePayrollReceipt> {

    private final PayrollRepository payrollRepository;

    public CalculatePayrollHandler(PayrollRepository payrollRepository) {
        this.payrollRepository = Objects.requireNonNull(payrollRepository, "payrollRepository");
    }

    @Override
    public Result<CalculatePayrollReceipt> handle(CalculatePayrollCommand command) {
        if (command == null) {
            return Result.failure("VALIDATION_ERROR", "Lệnh tính lương không được để trống.");
        }
        if (command.periodId() <= 0 || command.employeeId() <= 0) {
            return Result.failure("INVALID_PAYROLL_ID", "Mã kỳ lương và mã nhân viên phải lớn hơn 0.");
        }
        if (!isNonNegative(command.baseSalary())
                || !isNonNegative(command.salaryCoefficient())
                || !isNonNegative(command.totalAllowances())
                || !isNonNegative(command.totalExaminationFees())
                || !isNonNegative(command.totalDeductions())) {
            return Result.failure("INVALID_PAYROLL_AMOUNT", "Các thành phần lương phải được cung cấp và không âm.");
        }

        KyLuong period = payrollRepository.findPeriodById(command.periodId()).orElse(null);
        if (period == null) {
            return Result.failure("PAYROLL_PERIOD_NOT_FOUND", "Không tìm thấy kỳ lương.");
        }
        if (period.isDaChot()) {
            return Result.failure("PAYROLL_PERIOD_LOCKED", "Không thể tính lương cho kỳ đã chốt.");
        }

        BigDecimal netSalary = command.baseSalary()
                .multiply(command.salaryCoefficient())
                .add(command.totalAllowances())
                .add(command.totalExaminationFees())
                .subtract(command.totalDeductions());

        BangLuong payroll = new BangLuong();
        payroll.setMaKyLuong(command.periodId());
        payroll.setMaNhanVien(command.employeeId());
        payroll.setLuongCoBan(command.baseSalary());
        payroll.setHeSoLuong(command.salaryCoefficient());
        payroll.setTongPhuCap(command.totalAllowances());
        payroll.setTongThuLaoKham(command.totalExaminationFees());
        payroll.setTongKhauTru(command.totalDeductions());
        payroll.setThucLinh(netSalary);

        BangLuong saved = payrollRepository.savePayroll(payroll);
        if (saved == null) {
            return Result.failure("PAYROLL_SAVE_FAILED", "Không thể lưu bảng lương.");
        }
        return Result.success(new CalculatePayrollReceipt(
                saved.getId() != null ? saved.getId() : 0,
                saved.getMaKyLuong(),
                saved.getMaNhanVien(),
                saved.getLuongCoBan(),
                saved.getHeSoLuong(),
                saved.getTongPhuCap(),
                saved.getTongThuLaoKham(),
                saved.getTongKhauTru(),
                saved.getThucLinh()));
    }

    private boolean isNonNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }
}

package vn.clinicmanagement.application.payroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.payroll.BangLuong;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPayrollByPeriodHandlerTest {
    private PayrollHandlerTestSupport.InMemoryPayrollRepository repository;
    private GetPayrollByPeriodHandler handler;

    @BeforeEach
    void setUp() {
        repository = new PayrollHandlerTestSupport.InMemoryPayrollRepository();
        handler = new GetPayrollByPeriodHandler(repository);
        repository.periods.put(8, PayrollHandlerTestSupport.period(8, 10, 2026, true));
        addPayroll(8, 41, "1650000");
        addPayroll(8, 42, "2000000");
        addPayroll(9, 43, "800000");
        repository.periods.put(9, PayrollHandlerTestSupport.period(9, 11, 2026, false));
    }

    @Test
    void queriesSummaryByPeriodId() {
        Result<PayrollSummaryDto> result = handler.handle(new GetPayrollByPeriodQuery(8, null, null));

        assertTrue(result.isSuccess());
        PayrollSummaryDto summary = result.valueOrThrow();
        assertEquals(8, summary.periodId());
        assertEquals(10, summary.month());
        assertEquals(2026, summary.year());
        assertTrue(summary.locked());
        assertEquals(2, summary.payrollLines().size());
        assertEquals(new BigDecimal("3650000"), summary.totalNetSalary());
    }

    @Test
    void queriesSummaryByMonthAndYear() {
        Result<PayrollSummaryDto> result = handler.handle(new GetPayrollByPeriodQuery(null, 11, 2026));

        assertTrue(result.isSuccess());
        assertEquals(9, result.valueOrThrow().periodId());
        assertEquals(1, result.valueOrThrow().payrollLines().size());
        assertEquals(new BigDecimal("800000"), result.valueOrThrow().totalNetSalary());
    }

    @Test
    void rejectsAmbiguousPeriodSelector() {
        Result<PayrollSummaryDto> result = handler.handle(new GetPayrollByPeriodQuery(8, 10, 2026));

        assertTrue(result.isFailure());
        assertEquals("INVALID_PAYROLL_PERIOD_QUERY", result.getErrorCode().orElseThrow());
    }

    private void addPayroll(int periodId, int employeeId, String netSalary) {
        BangLuong payroll = new BangLuong();
        payroll.setId(repository.payrolls.size() + 1);
        payroll.setMaKyLuong(periodId);
        payroll.setMaNhanVien(employeeId);
        payroll.setLuongCoBan(BigDecimal.ZERO);
        payroll.setHeSoLuong(BigDecimal.ONE);
        payroll.setTongPhuCap(BigDecimal.ZERO);
        payroll.setTongThuLaoKham(BigDecimal.ZERO);
        payroll.setTongKhauTru(BigDecimal.ZERO);
        payroll.setThucLinh(new BigDecimal(netSalary));
        repository.payrolls.add(payroll);
    }
}

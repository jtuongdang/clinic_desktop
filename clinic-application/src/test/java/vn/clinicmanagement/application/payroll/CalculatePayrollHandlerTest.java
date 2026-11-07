package vn.clinicmanagement.application.payroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatePayrollHandlerTest {
    private PayrollHandlerTestSupport.InMemoryPayrollRepository repository;
    private CalculatePayrollHandler handler;

    @BeforeEach
    void setUp() {
        repository = new PayrollHandlerTestSupport.InMemoryPayrollRepository();
        handler = new CalculatePayrollHandler(repository);
        repository.periods.put(8, PayrollHandlerTestSupport.period(8, 10, 2026, false));
    }

    @Test
    void calculatesAndStoresNetSalary() {
        CalculatePayrollCommand command = new CalculatePayrollCommand(
                8, 41,
                new BigDecimal("1000000"),
                new BigDecimal("1.5"),
                new BigDecimal("200000"),
                new BigDecimal("50000"),
                new BigDecimal("100000"));

        Result<CalculatePayrollReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        CalculatePayrollReceipt receipt = result.valueOrThrow();
        assertEquals(new BigDecimal("1650000.0"), receipt.netSalary());
        assertEquals(1, repository.payrolls.size());
        assertEquals(receipt.netSalary(), repository.payrolls.get(0).getThucLinh());
        assertEquals(41, receipt.employeeId());
    }

    @Test
    void rejectsCalculationWhenPeriodIsLocked() {
        repository.periods.put(8, PayrollHandlerTestSupport.period(8, 10, 2026, true));

        Result<CalculatePayrollReceipt> result = handler.handle(new CalculatePayrollCommand(
                8, 41, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        assertTrue(result.isFailure());
        assertEquals("PAYROLL_PERIOD_LOCKED", result.getErrorCode().orElseThrow());
        assertTrue(repository.payrolls.isEmpty());
    }

    @Test
    void rejectsMissingOrNegativeAmounts() {
        Result<CalculatePayrollReceipt> missing = handler.handle(new CalculatePayrollCommand(
                8, 41, null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        Result<CalculatePayrollReceipt> negative = handler.handle(new CalculatePayrollCommand(
                8, 41, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("-1")));

        assertFalse(missing.isSuccess());
        assertEquals("INVALID_PAYROLL_AMOUNT", missing.getErrorCode().orElseThrow());
        assertEquals("INVALID_PAYROLL_AMOUNT", negative.getErrorCode().orElseThrow());
        assertTrue(repository.payrolls.isEmpty());
    }
}

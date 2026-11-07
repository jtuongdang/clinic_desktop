package vn.clinicmanagement.application.payroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockPayrollPeriodHandlerTest {
    private PayrollHandlerTestSupport.InMemoryPayrollRepository repository;
    private PayrollHandlerTestSupport.RecordingPayrollAuditPort auditPort;
    private LockPayrollPeriodHandler handler;

    @BeforeEach
    void setUp() {
        repository = new PayrollHandlerTestSupport.InMemoryPayrollRepository();
        auditPort = new PayrollHandlerTestSupport.RecordingPayrollAuditPort();
        handler = new LockPayrollPeriodHandler(repository, auditPort);
        repository.periods.put(8, PayrollHandlerTestSupport.period(8, 10, 2026, false));
    }

    @Test
    void adminLocksPeriodAndWritesAudit() {
        Result<LockPayrollPeriodReceipt> result = handler.handle(new LockPayrollPeriodCommand(
                8,
                PayrollHandlerTestSupport.authorization("ADMIN"),
                PayrollHandlerTestSupport.actor("ADMIN")));

        assertTrue(result.isSuccess());
        assertTrue(repository.periods.get(8).isDaChot());
        assertEquals(1, auditPort.entries.size());
        assertEquals(8, auditPort.entries.get(0).periodId());
        assertEquals("PAYROLL_PERIOD_LOCKED", auditPort.entries.get(0).actionCode());
    }

    @Test
    void hrAdminCanLockButOtherRolesAreDenied() {
        Result<LockPayrollPeriodReceipt> allowed = handler.handle(new LockPayrollPeriodCommand(
                8,
                PayrollHandlerTestSupport.authorization("HR_ADMIN"),
                PayrollHandlerTestSupport.actor("HR_ADMIN")));
        assertTrue(allowed.isSuccess());

        repository.periods.put(9, PayrollHandlerTestSupport.period(9, 11, 2026, false));
        Result<LockPayrollPeriodReceipt> denied = handler.handle(new LockPayrollPeriodCommand(
                9,
                PayrollHandlerTestSupport.authorization("DOCTOR"),
                PayrollHandlerTestSupport.actor("DOCTOR")));

        assertTrue(denied.isFailure());
        assertEquals("AUTHORIZATION_DENIED", denied.getErrorCode().orElseThrow());
        assertFalse(repository.periods.get(9).isDaChot());
        assertEquals(1, auditPort.entries.size());
    }
}

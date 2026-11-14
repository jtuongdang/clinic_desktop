package vn.clinicmanagement.application.payroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KhoaTienLuongKyHandlerTest {
    private TienLuongHandlerTestHoTro.InMemoryPayrollRepository repository;
    private TienLuongHandlerTestHoTro.RecordingPayrollAuditPort auditPort;
    private KhoaTienLuongKyHandler handler;

    @BeforeEach
    void setUp() {
        repository = new TienLuongHandlerTestHoTro.InMemoryPayrollRepository();
        auditPort = new TienLuongHandlerTestHoTro.RecordingPayrollAuditPort();
        handler = new KhoaTienLuongKyHandler(repository, auditPort);
        repository.periods.put(8, TienLuongHandlerTestHoTro.period(8, 10, 2026, false));
    }

    @Test
    void adminLocksPeriodAndWritesAudit() {
        KetQua<KhoaTienLuongKyBienNhan> result = handler.handle(new KhoaTienLuongKyCommand(
                8,
                TienLuongHandlerTestHoTro.authorization("ADMIN"),
                TienLuongHandlerTestHoTro.actor("ADMIN")));

        assertTrue(result.isSuccess());
        assertTrue(repository.periods.get(8).isDaChot());
        assertEquals(1, auditPort.entries.size());
        assertEquals(8, auditPort.entries.get(0).periodId());
        assertEquals("PAYROLL_PERIOD_LOCKED", auditPort.entries.get(0).actionCode());
    }

    @Test
    void hrAdminCanLockButOtherRolesAreDenied() {
        KetQua<KhoaTienLuongKyBienNhan> allowed = handler.handle(new KhoaTienLuongKyCommand(
                8,
                TienLuongHandlerTestHoTro.authorization("HR_ADMIN"),
                TienLuongHandlerTestHoTro.actor("HR_ADMIN")));
        assertTrue(allowed.isSuccess());

        repository.periods.put(9, TienLuongHandlerTestHoTro.period(9, 11, 2026, false));
        KetQua<KhoaTienLuongKyBienNhan> denied = handler.handle(new KhoaTienLuongKyCommand(
                9,
                TienLuongHandlerTestHoTro.authorization("DOCTOR"),
                TienLuongHandlerTestHoTro.actor("DOCTOR")));

        assertTrue(denied.isFailure());
        assertEquals("AUTHORIZATION_DENIED", denied.getErrorCode().orElseThrow());
        assertFalse(repository.periods.get(9).isDaChot());
        assertEquals(1, auditPort.entries.size());
    }
}

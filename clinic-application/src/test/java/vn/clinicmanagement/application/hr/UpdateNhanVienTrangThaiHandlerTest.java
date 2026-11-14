package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;

import static org.junit.jupiter.api.Assertions.*;

class UpdateNhanVienTrangThaiHandlerTest extends NhanSuHandlerTestHoTro {
    private UpdateNhanVienTrangThaiHandler handler() {
        return new UpdateNhanVienTrangThaiHandler(employees, authorization, auditLog, transactions);
    }

    @Test
    void deactivatesEmployeeWithDeactivationAudit() {
        employees.add(employee(1, "NV-1", "user", true));

        KetQua<UpdateNhanVienTrangThaiBienNhan> result = handler().handle(
                new UpdateNhanVienTrangThaiCommand(7, 1, false));

        assertTrue(result.isSuccess());
        assertFalse(employees.getById(1).orElseThrow().isHoatDong());
        assertEquals(NhanSuKiemToanHanhDong.USER_DEACTIVATED, audits.get(0).action());
    }

    @Test
    void activatesEmployeeWithUpdateAudit() {
        employees.add(employee(1, "NV-1", "user", false));

        KetQua<UpdateNhanVienTrangThaiBienNhan> result = handler().handle(
                new UpdateNhanVienTrangThaiCommand(7, 1, true));

        assertTrue(result.isSuccess());
        assertTrue(employees.getById(1).orElseThrow().isHoatDong());
        assertEquals(NhanSuKiemToanHanhDong.USER_UPDATED, audits.get(0).action());
    }

    @Test
    void rejectsUnauthorisedStatusChange() {
        employees.add(employee(1, "NV-1", "user", true));
        authorized = false;

        assertEquals("HR_FORBIDDEN", handler().handle(new UpdateNhanVienTrangThaiCommand(7, 1, false))
                .getError().orElseThrow().code());
        assertTrue(employees.getById(1).orElseThrow().isHoatDong());
        assertTrue(audits.isEmpty());
    }
}

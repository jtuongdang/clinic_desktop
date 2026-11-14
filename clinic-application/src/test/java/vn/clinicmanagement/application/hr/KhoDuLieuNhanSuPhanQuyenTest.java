package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KhoDuLieuNhanSuPhanQuyenTest extends NhanSuHandlerTestHoTro {
    @Test
    void allowsOnlyActiveAdminAndHrAdminAssignments() {
        employees.add(employee(1, "NV-1", "admin", true));
        employees.add(employee(2, "NV-2", "hr", true));
        employees.add(employee(3, "NV-3", "staff", true));
        employees.add(employee(4, "NV-4", "inactive", false));
        employees.setRoleNames(1, List.of("admin"));
        employees.setRoleNames(2, List.of("HR_ADMIN"));
        employees.setRoleNames(3, List.of("DOCTOR"));
        employees.setRoleNames(4, List.of("ADMIN"));
        KhoDuLieuNhanSuPhanQuyen authorization = new KhoDuLieuNhanSuPhanQuyen(employees);

        assertTrue(authorization.canManageEmployees(1));
        assertTrue(authorization.canManageEmployees(2));
        assertFalse(authorization.canManageEmployees(3));
        assertFalse(authorization.canManageEmployees(4));
        assertFalse(authorization.canManageEmployees(0));
    }
}

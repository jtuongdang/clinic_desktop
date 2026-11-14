package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.identity.NhanVien;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssignNhanVienVaiTroHandlerTest extends NhanSuHandlerTestHoTro {
    @Test
    void replacesRolesAndAuditsAssignment() {
        employees.add(employee(1, "NV-1", "user", true));
        List<Integer> assigned = new ArrayList<>();
        AssignNhanVienVaiTroHandler handler = new AssignNhanVienVaiTroHandler(employees,
                (employeeId, roleIds) -> assigned.addAll(roleIds), authorization, auditLog, transactions);

        var result = handler.handle(new AssignNhanVienVaiTroCommand(7, 1, List.of(2, 4)));

        assertTrue(result.isSuccess());
        assertEquals(List.of(2, 4), assigned);
        assertEquals(NhanSuKiemToanHanhDong.ROLE_ASSIGNMENT_CHANGED, audits.get(0).action());
        assertEquals(1, audits.get(0).employeeId());
    }

    @Test
    void rejectsInvalidRoleIdsBeforeChangingAssignments() {
        employees.add(employee(1, "NV-1", "user", true));
        List<Integer> assigned = new ArrayList<>();
        AssignNhanVienVaiTroHandler handler = new AssignNhanVienVaiTroHandler(employees,
                (employeeId, roleIds) -> assigned.addAll(roleIds), authorization, auditLog, transactions);

        assertEquals("HR_INVALID", handler.handle(
                new AssignNhanVienVaiTroCommand(7, 1, List.of(2, 2))).getError().orElseThrow().code());
        assertTrue(assigned.isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void rejectsNullRoleIdsBeforeChangingAssignments() {
        employees.add(employee(1, "NV-1", "user", true));
        List<Integer> roleIds = new ArrayList<>();
        roleIds.add(null);
        List<Integer> assigned = new ArrayList<>();
        AssignNhanVienVaiTroHandler handler = new AssignNhanVienVaiTroHandler(employees,
                (employeeId, assignedRoleIds) -> assigned.addAll(assignedRoleIds),
                authorization, auditLog, transactions);

        assertEquals("HR_INVALID", handler.handle(
                new AssignNhanVienVaiTroCommand(7, 1, roleIds)).getError().orElseThrow().code());
        assertTrue(assigned.isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void rejectsUnauthorisedRoleAssignment() {
        employees.add(employee(1, "NV-1", "user", true));
        authorized = false;
        AssignNhanVienVaiTroHandler handler = new AssignNhanVienVaiTroHandler(employees,
                (employeeId, roleIds) -> fail("Should not assign roles"), authorization, auditLog, transactions);

        assertEquals("HR_FORBIDDEN", handler.handle(
                new AssignNhanVienVaiTroCommand(7, 1, List.of(2))).getError().orElseThrow().code());
        assertTrue(audits.isEmpty());
    }
}

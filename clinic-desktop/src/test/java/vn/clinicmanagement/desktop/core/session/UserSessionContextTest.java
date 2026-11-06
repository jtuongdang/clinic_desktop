package vn.clinicmanagement.desktop.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionContextTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final UserSession session = UserSession.getInstance();

    @AfterEach
    void clearSessionAfterEachTest() { session.clearSession(); }

    @Test
    void storesExactlyTheSelectedRoleAndWorkScopeIdsAndNames() {
        RoleChoice selectedRole = new RoleChoice(42, "DOCTOR", "Bác sĩ điều trị");
        WorkScopeChoice selectedScope = new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu");

        session.setSession(701, "synth.user.701", "Nhân viên mẫu", selectedRole, selectedScope);

        assertTrue(session.isLoggedOn());
        assertEquals(701, session.getUserId());
        assertEquals("synth.user.701", session.getUsername());
        assertEquals("Nhân viên mẫu", session.getFullName());
        assertEquals(42, session.getRoleId());
        assertEquals("DOCTOR", session.getRoleCode());
        assertEquals("Bác sĩ điều trị", session.getRoleName());
        assertEquals(11, session.getFacilityId());
        assertEquals("Cơ sở mẫu", session.getFacilityName());
        assertEquals(21, session.getDepartmentId());
        assertEquals("Khoa mẫu", session.getDepartmentName());
        assertEquals("", session.getStationId());
        assertEquals("", session.getWorkstationId());
    }

    @Test
    void stationAndWorkstationRemainIndependentFromSelectedScope() {
        session.setSession(701, "synth.user.701", "Nhân viên mẫu",
                new RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu"));
        session.setStationId("station-A");
        session.setWorkstationId("workstation-7");

        assertEquals("station-A", session.getStationId());
        assertEquals("workstation-7", session.getWorkstationId());
        assertEquals(11, session.getFacilityId());
        assertEquals(21, session.getDepartmentId());
    }

    @Test
    void logoutClearsIdentityRoleScopeStationAndWorkstationContext() {
        session.setSession(701, "synth.user.701", "Nhân viên mẫu",
                new RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu"));
        session.setStationId("station-A");
        session.setWorkstationId("workstation-7");

        session.clearSession();

        assertFalse(session.isLoggedOn());
        assertEquals(0, session.getUserId());
        assertEquals("", session.getUsername());
        assertEquals("", session.getFullName());
        assertEquals(0, session.getRoleId());
        assertEquals("", session.getRoleCode());
        assertEquals("", session.getRoleName());
        assertEquals(0, session.getFacilityId());
        assertEquals("", session.getFacilityName());
        assertEquals(0, session.getDepartmentId());
        assertEquals("", session.getDepartmentName());
        assertEquals("", session.getStationId());
        assertEquals("", session.getWorkstationId());
    }

    @Test
    void startingANewSessionClearsStationAndWorkstationFromPreviousSession() {
        session.setSession(701, "synth.user.701", "Nhân viên mẫu",
                new RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu"));
        session.setStationId("station-A");
        session.setWorkstationId("workstation-7");

        session.setSession(702, "synth.user.702", "Nhân viên khác",
                new RoleChoice(43, "RECEPTION", "Tiếp đón"),
                new WorkScopeChoice(12, "Cơ sở khác", 22, "Khoa khác"));

        assertTrue(session.isLoggedOn());
        assertEquals(702, session.getUserId());
        assertEquals(12, session.getFacilityId());
        assertEquals(22, session.getDepartmentId());
        assertEquals("", session.getStationId());
        assertEquals("", session.getWorkstationId());
    }

    @Test
    void choiceLabelsShowNamesAndRoleCodeWithoutInternalIds() {
        RoleChoice role = new RoleChoice(42, "DOCTOR", "Bác sĩ điều trị");
        WorkScopeChoice scope = new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu");

        assertEquals("Bác sĩ điều trị (DOCTOR)", role.toString());
        assertEquals("Cơ sở mẫu — Khoa mẫu", scope.toString());
        assertFalse(role.toString().contains("42"));
        assertFalse(scope.toString().contains("11"));
        assertFalse(scope.toString().contains("21"));
    }

    @Test
    void refusesToCreateSessionWithoutBothSelectedChoices() {
        assertThrows(IllegalArgumentException.class,
                () -> session.setSession(701, "synth.user.701", "Nhân viên mẫu", null,
                        new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu")));
        assertFalse(session.isLoggedOn());
    }
    @Test void authorityCanOnlyBeChangedThroughTypedLifecycleAndNotPublicSetters() {
        java.util.Set<String> forbidden = java.util.Set.of("setSession", "setUserId", "setUsername", "setFullName",
                "setLoggedOn", "setRole", "setRoleId", "setRoleCode", "setFacilityId", "setFacilityName", "setDepartmentId", "setDepartmentName");
        for (var method : UserSession.class.getMethods()) assertFalse(forbidden.contains(method.getName()), method.getName());
        for (var method : SessionLifecycleService.class.getMethods()) {
            if (method.getName().equals("startSession")) assertArrayEquals(
                    new Class<?>[]{vn.clinicmanagement.application.auth.CompletedLoginSession.class}, method.getParameterTypes());
        }
        for (var constructor : SessionLifecycleService.class.getConstructors()) assertTrue(
                java.util.List.of(constructor.getParameterTypes()).contains(vn.clinicmanagement.application.audit.AuditWritePort.class));
    }

}

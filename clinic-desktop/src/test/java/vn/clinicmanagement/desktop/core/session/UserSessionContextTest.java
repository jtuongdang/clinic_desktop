package vn.clinicmanagement.desktop.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionContextTest {
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
        assertEquals("21", session.getStationId());
    }

    @Test
    void logoutClearsIdentityRoleAndScopeContext() {
        session.setSession(701, "synth.user.701", "Nhân viên mẫu",
                new RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu"));

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
    }

    @Test
    void refusesToCreateSessionWithoutBothSelectedChoices() {
        assertThrows(IllegalArgumentException.class,
                () -> session.setSession(701, "synth.user.701", "Nhân viên mẫu", null,
                        new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu")));
        assertFalse(session.isLoggedOn());
    }
}

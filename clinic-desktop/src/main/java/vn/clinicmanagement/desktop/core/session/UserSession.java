package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;

public class UserSession {
    private static UserSession instance;

    private boolean loggedOn;
    private int userId;
    private String username;
    private String fullName;
    private int roleId;
    private String roleCode;
    private String roleName;
    private int facilityId;
    private String facilityName;
    private int departmentId;
    private String departmentName;

    private UserSession() { clearSession(); }

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setSession(int userId, String username, String fullName, RoleChoice role, WorkScopeChoice scope) {
        if (role == null || scope == null) throw new IllegalArgumentException("Role and work scope must be selected.");
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.roleId = role.id();
        this.roleCode = role.code();
        this.roleName = role.name();
        this.facilityId = scope.facilityId();
        this.facilityName = scope.facilityName();
        this.departmentId = scope.departmentId();
        this.departmentName = scope.departmentName();
        this.loggedOn = true;
    }

    public void clearSession() {
        loggedOn = false;
        userId = 0;
        username = "";
        fullName = "";
        roleId = 0;
        roleCode = "";
        roleName = "";
        facilityId = 0;
        facilityName = "";
        departmentId = 0;
        departmentName = "";
    }

    public boolean isLoggedOn() { return loggedOn; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public int getRoleId() { return roleId; }
    public String getRoleCode() { return roleCode; }
    public String getRoleName() { return roleName; }
    /** Kept for existing dashboard/status bar consumers. */
    public String getRole() { return roleName; }
    public int getFacilityId() { return facilityId; }
    public String getFacilityName() { return facilityName; }
    public int getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    /** Compatibility view for existing dashboard labels; the selected department is the current station context. */
    public String getStationId() { return departmentId == 0 ? "" : Integer.toString(departmentId); }
}

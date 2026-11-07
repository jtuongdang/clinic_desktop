package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;

import java.time.Instant;

public final class UserSession {
    private static UserSession instance;

    private volatile boolean loggedOn;
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
    private String stationId;
    private String workstationId;
    private String sessionId;
    private Instant issuedAt;
    private Instant lastActivityAt;
    private Instant absoluteExpiresAt;
    private boolean locked;
    private boolean absoluteExpiryPending;

    private UserSession() { clearSession(); }

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    void setSession(int userId, String username, String fullName, RoleChoice role, WorkScopeChoice scope) {
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
        this.stationId = "";
        this.workstationId = "";
        this.sessionId = null;
        this.issuedAt = null;
        this.lastActivityAt = null;
        this.absoluteExpiresAt = null;
        this.locked = false;
        this.absoluteExpiryPending = false;
        this.loggedOn = true;
    }

    void setLifecycleMetadata(String sessionId, Instant issuedAt, Instant lastActivityAt, Instant absoluteExpiresAt) {
        if (sessionId == null || issuedAt == null || lastActivityAt == null || absoluteExpiresAt == null
                || !absoluteExpiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("A bounded session lifecycle is required.");
        }
        this.sessionId = sessionId;
        this.issuedAt = issuedAt;
        this.lastActivityAt = lastActivityAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
        this.locked = false;
        this.absoluteExpiryPending = false;
    }

    void setLastActivityAt(Instant value) { lastActivityAt = value; }
    void setLocked(boolean value, boolean absoluteExpiryPending) {
        locked = value;
        this.absoluteExpiryPending = value && absoluteExpiryPending;
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
        stationId = "";
        workstationId = "";
        sessionId = null;
        issuedAt = null;
        lastActivityAt = null;
        absoluteExpiresAt = null;
        locked = false;
        absoluteExpiryPending = false;
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
    public String getStationId() { return stationId; }
    void setStationId(String stationId) { this.stationId = stationId; }
    public String getWorkstationId() { return workstationId; }
    void setWorkstationId(String workstationId) { this.workstationId = workstationId; }
    public String getSessionId() { return sessionId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getAbsoluteExpiresAt() { return absoluteExpiresAt; }
    public boolean isLocked() { return locked; }
    public boolean isAbsoluteExpiryPending() { return absoluteExpiryPending; }
}

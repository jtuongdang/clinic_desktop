package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;

import java.time.Instant;

public final class NguoiDungPhien {
    private static NguoiDungPhien instance;

    /** Trường `loggedOn`: `logged` tải. */
    private volatile boolean loggedOn;
    /** Trường `userId`: người dùng mã. */
    private int userId;
    private String username;
    /** Trường `fullName`: `full` tên. */
    private String fullName;
    /** Trường `roleId`: vai trò mã. */
    private int roleId;
    /** Trường `roleCode`: vai trò mã. */
    private String roleCode;
    /** Trường `roleName`: vai trò tên. */
    private String roleName;
    /** Trường `facilityId`: cơ sở mã. */
    private int facilityId;
    /** Trường `facilityName`: cơ sở tên. */
    private String facilityName;
    /** Trường `departmentId`: khoa mã. */
    private int departmentId;
    /** Trường `departmentName`: khoa tên. */
    private String departmentName;
    /** Trường `stationId`: tram mã. */
    private String stationId;
    /** Trường `workstationId`: `workstation` mã. */
    private String workstationId;
    /** Trường `sessionId`: phien mã. */
    private String sessionId;
    /** Trường `issuedAt`: `issued` luc. */
    private Instant issuedAt;
    /** Trường `lastActivityAt`: `last` `Activity` luc. */
    private Instant lastActivityAt;
    /** Trường `absoluteExpiresAt`: `absolute` `Expires` luc. */
    private Instant absoluteExpiresAt;
    /** Trường `locked`: dakhoa. */
    private boolean locked;
    private boolean absoluteExpiryPending;

    private NguoiDungPhien() { clearSession(); }

    /** Hàm `getInstance`: lấy `Instance`. */
    public static synchronized NguoiDungPhien getInstance() {
        if (instance == null) instance = new NguoiDungPhien();
        return instance;
    }

    /** Hàm `setSession`: gán phien. */
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

    /** Hàm `setLifecycleMetadata`: gán vongdoi sieudulieu. */
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

    /** Hàm `setLastActivityAt`: gán `Last` `Activity` luc. */
    void setLastActivityAt(Instant value) { lastActivityAt = value; }
    /** Hàm `setLocked`: gán dakhoa. */
    void setLocked(boolean value, boolean absoluteExpiryPending) {
        locked = value;
        this.absoluteExpiryPending = value && absoluteExpiryPending;
    }

    /** Hàm `clearSession`: xóa phien. */
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

    /** Hàm `isLoggedOn`: là `Logged` tải. */
    public boolean isLoggedOn() { return loggedOn; }
    /** Hàm `getUserId`: lấy người dùng mã. */
    public int getUserId() { return userId; }
    /** Hàm `getUsername`: lấy `Username`. */
    public String getUsername() { return username; }
    /** Hàm `getFullName`: lấy `Full` tên. */
    public String getFullName() { return fullName; }
    /** Hàm `getRoleId`: lấy vai trò mã. */
    public int getRoleId() { return roleId; }
    /** Hàm `getRoleCode`: lấy vai trò mã. */
    public String getRoleCode() { return roleCode; }
    /** Hàm `getRoleName`: lấy vai trò tên. */
    public String getRoleName() { return roleName; }
    /** Kept for existing dashboard/status bar consumers. */
    public String getRole() { return roleName; }
    /** Hàm `getFacilityId`: lấy cơ sở mã. */
    public int getFacilityId() { return facilityId; }
    /** Hàm `getFacilityName`: lấy cơ sở tên. */
    public String getFacilityName() { return facilityName; }
    /** Hàm `getDepartmentId`: lấy khoa mã. */
    public int getDepartmentId() { return departmentId; }
    /** Hàm `getDepartmentName`: lấy khoa tên. */
    public String getDepartmentName() { return departmentName; }
    /** Hàm `getStationId`: lấy tram mã. */
    public String getStationId() { return stationId; }
    /** Hàm `setStationId`: gán tram mã. */
    void setStationId(String stationId) { this.stationId = stationId; }
    /** Hàm `getWorkstationId`: lấy `Workstation` mã. */
    public String getWorkstationId() { return workstationId; }
    /** Hàm `setWorkstationId`: gán `Workstation` mã. */
    void setWorkstationId(String workstationId) { this.workstationId = workstationId; }
    /** Hàm `getSessionId`: lấy phien mã. */
    public String getSessionId() { return sessionId; }
    /** Hàm `getIssuedAt`: lấy `Issued` luc. */
    public Instant getIssuedAt() { return issuedAt; }
    /** Hàm `getLastActivityAt`: lấy `Last` `Activity` luc. */
    public Instant getLastActivityAt() { return lastActivityAt; }
    /** Hàm `getAbsoluteExpiresAt`: lấy `Absolute` `Expires` luc. */
    public Instant getAbsoluteExpiresAt() { return absoluteExpiresAt; }
    /** Hàm `isLocked`: là dakhoa. */
    public boolean isLocked() { return locked; }
    /** Hàm `isAbsoluteExpiryPending`: là `Absolute` `Expiry` `Pending`. */
    public boolean isAbsoluteExpiryPending() { return absoluteExpiryPending; }
}

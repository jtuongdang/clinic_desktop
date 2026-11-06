package vn.clinicmanagement.desktop.core.session;

public class UserSession {
    private static UserSession instance;

    private boolean loggedOn;
    private int userId;
    private String username;
    private String fullName;
    private String role;
    private String facilityName;
    private String departmentName;
    private String stationId;
    private String workstationId;

    private UserSession() {
        clearSession();
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setSession(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.loggedOn = true;
    }

    public void clearSession() {
        this.loggedOn = false;
        this.userId = 0;
        this.username = "";
        this.fullName = "";
        this.role = "";
        this.facilityName = "";
        this.departmentName = "";
        this.stationId = "";
        this.workstationId = "";
    }

    public boolean isLoggedOn() { return loggedOn; }
    public void setLoggedOn(boolean loggedOn) { this.loggedOn = loggedOn; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getWorkstationId() { return workstationId; }
    public void setWorkstationId(String workstationId) { this.workstationId = workstationId; }
}

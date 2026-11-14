package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Truy vấn thống kê lượt khám bệnh và phân bố khoa/phòng (RPT-01).
 * Thẩm quyền truy cập: ADMIN, DIRECTOR, DOCTOR.
 */
public class GetLuotKhamThongKeQuery implements Query<LuotKhamThongKeDuLieu> {

    /** Trường `currentUserRole`: hientai người dùng vai trò. */
    private final String currentUserRole;
    /** Trường `fromDate`: tu ngày. */
    private final LocalDate fromDate;
    /** Trường `toDate`: den ngày. */
    private final LocalDate toDate;
    /** Trường `facilityId`: cơ sở mã. */
    private final Integer facilityId;
    /** Trường `departmentId`: khoa mã. */
    private final Integer departmentId;
    /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
    private final PhanQuyenNguCanh authorizationContext;
    /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
    private final KiemToanTacNhanNguCanh auditActorContext;

    public GetLuotKhamThongKeQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer facilityId, Integer departmentId,
                                   PhanQuyenNguCanh authorizationContext,
                                   KiemToanTacNhanNguCanh auditActorContext) {
        this.currentUserRole = currentUserRole;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.facilityId = facilityId;
        this.departmentId = departmentId;
        this.authorizationContext = authorizationContext;
        this.auditActorContext = auditActorContext;
    }

    public GetLuotKhamThongKeQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer facilityId, Integer departmentId) {
        this(currentUserRole, fromDate, toDate, facilityId, departmentId, null, null);
    }

    public GetLuotKhamThongKeQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate) {
        this(currentUserRole, fromDate, toDate, null, null, null, null);
    }

    public GetLuotKhamThongKeQuery(String currentUserRole, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole,
                fromTime != null ? fromTime.toLocalDate() : null,
                toTime != null ? toTime.toLocalDate() : null,
                null, null, null, null);
    }

    public GetLuotKhamThongKeQuery(String currentUserRole) {
        this(currentUserRole, null, null, null, null, null, null);
    }

    public GetLuotKhamThongKeQuery(PhanQuyenNguCanh authContext, LocalDate fromDate, LocalDate toDate) {
        this(authContext != null ? authContext.roleCode() : null,
                fromDate, toDate,
                authContext != null ? authContext.facilityId() : null,
                authContext != null ? authContext.departmentId() : null,
                authContext, null);
    }

    public GetLuotKhamThongKeQuery(PhanQuyenNguCanh authContext, KiemToanTacNhanNguCanh auditContext,
                                   LocalDate fromDate, LocalDate toDate) {
        this(authContext != null ? authContext.roleCode() : null,
                fromDate, toDate,
                authContext != null ? authContext.facilityId() : null,
                authContext != null ? authContext.departmentId() : null,
                authContext, auditContext);
    }

    /** Hàm `getEffectiveRole`: lấy `Effective` vai trò. */
    public String getEffectiveRole() {
        if (currentUserRole != null && !currentUserRole.isBlank()) {
            return currentUserRole.trim().toUpperCase(Locale.ROOT);
        }
        if (authorizationContext != null && authorizationContext.roleCode() != null && !authorizationContext.roleCode().isBlank()) {
            return authorizationContext.roleCode().trim().toUpperCase(Locale.ROOT);
        }
        if (auditActorContext != null && auditActorContext.roleCode() != null && !auditActorContext.roleCode().isBlank()) {
            return auditActorContext.roleCode().trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    /** Hàm `getCurrentUserRole`: lấy hientai người dùng vai trò. */
    public String getCurrentUserRole() { return currentUserRole; }
    /** Hàm `currentUserRole`: hientai người dùng vai trò. */
    public String currentUserRole() { return currentUserRole; }
    /** Hàm `getRole`: lấy vai trò. */
    public String getRole() { return currentUserRole; }
    /** Hàm `role`: vai trò. */
    public String role() { return currentUserRole; }

    /** Hàm `getFromDate`: lấy tu ngày. */
    public LocalDate getFromDate() { return fromDate; }
    /** Hàm `fromDate`: tu ngày. */
    public LocalDate fromDate() { return fromDate; }
    /** Hàm `getTuNgay`: lấy tu `Ngay`. */
    public LocalDate getTuNgay() { return fromDate; }
    public LocalDate tuNgay() { return fromDate; }

    /** Hàm `getToDate`: lấy den ngày. */
    public LocalDate getToDate() { return toDate; }
    /** Hàm `toDate`: den ngày. */
    public LocalDate toDate() { return toDate; }
    /** Hàm `getDenNgay`: lấy `Den` `Ngay`. */
    public LocalDate getDenNgay() { return toDate; }
    public LocalDate denNgay() { return toDate; }

    /** Hàm `getFacilityId`: lấy cơ sở mã. */
    public Integer getFacilityId() { return facilityId; }
    /** Hàm `facilityId`: cơ sở mã. */
    public Integer facilityId() { return facilityId; }

    /** Hàm `getDepartmentId`: lấy khoa mã. */
    public Integer getDepartmentId() { return departmentId; }
    /** Hàm `departmentId`: khoa mã. */
    public Integer departmentId() { return departmentId; }

    /** Hàm `getAuthorizationContext`: lấy phanquyen ngữ cảnh. */
    public PhanQuyenNguCanh getAuthorizationContext() { return authorizationContext; }
    /** Hàm `authorizationContext`: phanquyen ngữ cảnh. */
    public PhanQuyenNguCanh authorizationContext() { return authorizationContext; }

    /** Hàm `getAuditActorContext`: lấy kiểm toán tacnhan ngữ cảnh. */
    public KiemToanTacNhanNguCanh getAuditActorContext() { return auditActorContext; }
    /** Hàm `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
    public KiemToanTacNhanNguCanh auditActorContext() { return auditActorContext; }
}

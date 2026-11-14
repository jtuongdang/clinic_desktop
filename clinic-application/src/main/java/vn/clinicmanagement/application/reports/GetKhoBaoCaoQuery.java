package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Truy vấn báo cáo xuất nhập tồn kho dược & cảnh báo hết hạn (RPT-01).
 * Thẩm quyền truy cập: ADMIN, DIRECTOR, PHARMACIST.
 */
public class GetKhoBaoCaoQuery implements Query<KhoBaoCaoDuLieu> {

    public static final int DEFAULT_EXPIRY_ALERT_DAYS = 180; // 6 tháng

    /** Trường `currentUserRole`: hientai người dùng vai trò. */
    private final String currentUserRole;
    /** Trường `fromDate`: tu ngày. */
    private final LocalDate fromDate;
    /** Trường `toDate`: den ngày. */
    private final LocalDate toDate;
    /** Trường `warehouseId`: kho mã. */
    private final Integer warehouseId;
    private final int expiryAlertDays;
    /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
    private final PhanQuyenNguCanh authorizationContext;
    /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
    private final KiemToanTacNhanNguCanh auditActorContext;

    public GetKhoBaoCaoQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer warehouseId, int expiryAlertDays,
                                   PhanQuyenNguCanh authorizationContext,
                                   KiemToanTacNhanNguCanh auditActorContext) {
        this.currentUserRole = currentUserRole;
        this.fromDate = fromDate;
        this.toDate = toDate != null ? toDate : LocalDate.now();
        this.warehouseId = warehouseId;
        this.expiryAlertDays = expiryAlertDays > 0 ? expiryAlertDays : DEFAULT_EXPIRY_ALERT_DAYS;
        this.authorizationContext = authorizationContext;
        this.auditActorContext = auditActorContext;
    }

    public GetKhoBaoCaoQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer warehouseId, int expiryAlertDays) {
        this(currentUserRole, fromDate, toDate, warehouseId, expiryAlertDays, null, null);
    }

    public GetKhoBaoCaoQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate, Integer warehouseId) {
        this(currentUserRole, fromDate, toDate, warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetKhoBaoCaoQuery(String currentUserRole, Integer warehouseId, int expiryAlertDays) {
        this(currentUserRole, null, LocalDate.now(), warehouseId, expiryAlertDays, null, null);
    }

    public GetKhoBaoCaoQuery(String currentUserRole, Integer warehouseId) {
        this(currentUserRole, null, LocalDate.now(), warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetKhoBaoCaoQuery(String currentUserRole) {
        this(currentUserRole, null, LocalDate.now(), null, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetKhoBaoCaoQuery(String currentUserRole, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole,
                fromTime != null ? fromTime.toLocalDate() : null,
                toTime != null ? toTime.toLocalDate() : LocalDate.now(),
                null, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetKhoBaoCaoQuery(PhanQuyenNguCanh authContext, Integer warehouseId) {
        this(authContext != null ? authContext.roleCode() : null,
                null, LocalDate.now(), warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, authContext, null);
    }

    public GetKhoBaoCaoQuery(PhanQuyenNguCanh authContext, KiemToanTacNhanNguCanh auditContext, Integer warehouseId) {
        this(authContext != null ? authContext.roleCode() : null,
                null, LocalDate.now(), warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, authContext, auditContext);
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

    /** Hàm `getWarehouseId`: lấy kho mã. */
    public Integer getWarehouseId() { return warehouseId; }
    /** Hàm `warehouseId`: kho mã. */
    public Integer warehouseId() { return warehouseId; }
    /** Hàm `getKhoId`: lấy kho mã. */
    public Integer getKhoId() { return warehouseId; }
    /** Hàm `khoId`: `kho` mã. */
    public Integer khoId() { return warehouseId; }

    /** Hàm `getExpiryAlertDays`: lấy `Expiry` `Alert` `Days`. */
    public int getExpiryAlertDays() { return expiryAlertDays; }
    public int expiryAlertDays() { return expiryAlertDays; }

    /** Hàm `getAuthorizationContext`: lấy phanquyen ngữ cảnh. */
    public PhanQuyenNguCanh getAuthorizationContext() { return authorizationContext; }
    /** Hàm `authorizationContext`: phanquyen ngữ cảnh. */
    public PhanQuyenNguCanh authorizationContext() { return authorizationContext; }

    /** Hàm `getAuditActorContext`: lấy kiểm toán tacnhan ngữ cảnh. */
    public KiemToanTacNhanNguCanh getAuditActorContext() { return auditActorContext; }
    /** Hàm `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
    public KiemToanTacNhanNguCanh auditActorContext() { return auditActorContext; }
}

package vn.clinicmanagement.application.reports.get;

import vn.clinicmanagement.application.reports.KhoBaoCaoDuLieu;

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

    private final String currentUserRole;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer warehouseId;
    private final int expiryAlertDays;
    private final PhanQuyenNguCanh authorizationContext;
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

    public String getCurrentUserRole() { return currentUserRole; }
    public String currentUserRole() { return currentUserRole; }
    public String getRole() { return currentUserRole; }
    public String role() { return currentUserRole; }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate fromDate() { return fromDate; }
    public LocalDate getTuNgay() { return fromDate; }
    public LocalDate tuNgay() { return fromDate; }

    public LocalDate getToDate() { return toDate; }
    public LocalDate toDate() { return toDate; }
    public LocalDate getDenNgay() { return toDate; }
    public LocalDate denNgay() { return toDate; }

    public Integer getWarehouseId() { return warehouseId; }
    public Integer warehouseId() { return warehouseId; }
    public Integer getKhoId() { return warehouseId; }
    public Integer khoId() { return warehouseId; }

    public int getExpiryAlertDays() { return expiryAlertDays; }
    public int expiryAlertDays() { return expiryAlertDays; }

    public PhanQuyenNguCanh getAuthorizationContext() { return authorizationContext; }
    public PhanQuyenNguCanh authorizationContext() { return authorizationContext; }

    public KiemToanTacNhanNguCanh getAuditActorContext() { return auditActorContext; }
    public KiemToanTacNhanNguCanh auditActorContext() { return auditActorContext; }
}

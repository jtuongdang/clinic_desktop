package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Truy vấn báo cáo xuất nhập tồn kho dược & cảnh báo hết hạn (RPT-01).
 * Thẩm quyền truy cập: ADMIN, DIRECTOR, PHARMACIST.
 */
public class GetInventoryReportQuery implements Query<InventoryReportDto> {

    public static final int DEFAULT_EXPIRY_ALERT_DAYS = 180; // 6 tháng

    private final String currentUserRole;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer warehouseId;
    private final int expiryAlertDays;
    private final AuthorizationContext authorizationContext;
    private final AuditActorContext auditActorContext;

    public GetInventoryReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer warehouseId, int expiryAlertDays,
                                   AuthorizationContext authorizationContext,
                                   AuditActorContext auditActorContext) {
        this.currentUserRole = currentUserRole;
        this.fromDate = fromDate;
        this.toDate = toDate != null ? toDate : LocalDate.now();
        this.warehouseId = warehouseId;
        this.expiryAlertDays = expiryAlertDays > 0 ? expiryAlertDays : DEFAULT_EXPIRY_ALERT_DAYS;
        this.authorizationContext = authorizationContext;
        this.auditActorContext = auditActorContext;
    }

    public GetInventoryReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                   Integer warehouseId, int expiryAlertDays) {
        this(currentUserRole, fromDate, toDate, warehouseId, expiryAlertDays, null, null);
    }

    public GetInventoryReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate, Integer warehouseId) {
        this(currentUserRole, fromDate, toDate, warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetInventoryReportQuery(String currentUserRole, Integer warehouseId, int expiryAlertDays) {
        this(currentUserRole, null, LocalDate.now(), warehouseId, expiryAlertDays, null, null);
    }

    public GetInventoryReportQuery(String currentUserRole, Integer warehouseId) {
        this(currentUserRole, null, LocalDate.now(), warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetInventoryReportQuery(String currentUserRole) {
        this(currentUserRole, null, LocalDate.now(), null, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetInventoryReportQuery(String currentUserRole, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole,
                fromTime != null ? fromTime.toLocalDate() : null,
                toTime != null ? toTime.toLocalDate() : LocalDate.now(),
                null, DEFAULT_EXPIRY_ALERT_DAYS, null, null);
    }

    public GetInventoryReportQuery(AuthorizationContext authContext, Integer warehouseId) {
        this(authContext != null ? authContext.roleCode() : null,
                null, LocalDate.now(), warehouseId, DEFAULT_EXPIRY_ALERT_DAYS, authContext, null);
    }

    public GetInventoryReportQuery(AuthorizationContext authContext, AuditActorContext auditContext, Integer warehouseId) {
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

    public AuthorizationContext getAuthorizationContext() { return authorizationContext; }
    public AuthorizationContext authorizationContext() { return authorizationContext; }

    public AuditActorContext getAuditActorContext() { return auditActorContext; }
    public AuditActorContext auditActorContext() { return auditActorContext; }
}

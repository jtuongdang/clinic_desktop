package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Truy vấn báo cáo doanh thu tài chính (RPT-01).
 * Thẩm quyền truy cập: ADMIN, DIRECTOR, ACCOUNTANT.
 */
public class GetRevenueReportQuery implements Query<RevenueReportDto> {

    private final String currentUserRole;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer facilityId;
    private final Integer departmentId;
    private final AuthorizationContext authorizationContext;
    private final AuditActorContext auditActorContext;

    public GetRevenueReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                 Integer facilityId, Integer departmentId,
                                 AuthorizationContext authorizationContext,
                                 AuditActorContext auditActorContext) {
        this.currentUserRole = currentUserRole;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.facilityId = facilityId;
        this.departmentId = departmentId;
        this.authorizationContext = authorizationContext;
        this.auditActorContext = auditActorContext;
    }

    public GetRevenueReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate,
                                 Integer facilityId, Integer departmentId) {
        this(currentUserRole, fromDate, toDate, facilityId, departmentId, null, null);
    }

    public GetRevenueReportQuery(String currentUserRole, LocalDate fromDate, LocalDate toDate) {
        this(currentUserRole, fromDate, toDate, null, null, null, null);
    }

    public GetRevenueReportQuery(String currentUserRole, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole,
                fromTime != null ? fromTime.toLocalDate() : null,
                toTime != null ? toTime.toLocalDate() : null,
                null, null, null, null);
    }

    public GetRevenueReportQuery(String currentUserRole) {
        this(currentUserRole, null, null, null, null, null, null);
    }

    public GetRevenueReportQuery(AuthorizationContext authContext, LocalDate fromDate, LocalDate toDate) {
        this(authContext != null ? authContext.roleCode() : null,
                fromDate, toDate,
                authContext != null ? authContext.facilityId() : null,
                authContext != null ? authContext.departmentId() : null,
                authContext, null);
    }

    public GetRevenueReportQuery(AuthorizationContext authContext, AuditActorContext auditContext,
                                 LocalDate fromDate, LocalDate toDate) {
        this(authContext != null ? authContext.roleCode() : null,
                fromDate, toDate,
                authContext != null ? authContext.facilityId() : null,
                authContext != null ? authContext.departmentId() : null,
                authContext, auditContext);
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

    public Integer getFacilityId() { return facilityId; }
    public Integer facilityId() { return facilityId; }

    public Integer getDepartmentId() { return departmentId; }
    public Integer departmentId() { return departmentId; }

    public AuthorizationContext getAuthorizationContext() { return authorizationContext; }
    public AuthorizationContext authorizationContext() { return authorizationContext; }

    public AuditActorContext getAuditActorContext() { return auditActorContext; }
    public AuditActorContext auditActorContext() { return auditActorContext; }
}

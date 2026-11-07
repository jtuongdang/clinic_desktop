package vn.clinicmanagement.application.audit;

import vn.clinicmanagement.application.common.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Truy vấn tra cứu nhật ký kiểm toán (AUD-01).
 * Hỗ trợ lọc theo actor, resourceType, actionCode, khoảng thời gian và phân trang.
 * Yêu cầu thẩm quyền ADMIN hoặc AUDITOR.
 */
public class GetAuditLogsQuery implements Query<List<AuditLogDto>> {
    private final String currentUserRole;
    private final String actor;
    private final String resourceType;
    private final String actionCode;
    private final LocalDateTime fromTime;
    private final LocalDateTime toTime;
    private final int page;
    private final int pageSize;

    public GetAuditLogsQuery(String currentUserRole, String actor, String resourceType,
                             String actionCode, LocalDateTime fromTime, LocalDateTime toTime,
                             int page, int pageSize) {
        this.currentUserRole = currentUserRole;
        this.actor = actor;
        this.resourceType = resourceType;
        this.actionCode = actionCode;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.page = Math.max(page, 0);
        this.pageSize = pageSize > 0 ? pageSize : 50;
    }

    public GetAuditLogsQuery(String currentUserRole, String actor, String resourceType,
                             String actionCode, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole, actor, resourceType, actionCode, fromTime, toTime, 0, 50);
    }

    public GetAuditLogsQuery(String currentUserRole) {
        this(currentUserRole, null, null, null, null, null, 0, 50);
    }

    public GetAuditLogsQuery(String actor, String resourceType, String actionCode,
                             LocalDateTime fromTime, LocalDateTime toTime) {
        this("ADMIN", actor, resourceType, actionCode, fromTime, toTime, 0, 50);
    }

    public String getCurrentUserRole() { return currentUserRole; }
    public String currentUserRole() { return currentUserRole; }
    public String getActorRole() { return currentUserRole; }
    public String actorRole() { return currentUserRole; }
    public String getCallerRole() { return currentUserRole; }
    public String callerRole() { return currentUserRole; }
    public String getRole() { return currentUserRole; }
    public String role() { return currentUserRole; }

    public String getActor() { return actor; }
    public String actor() { return actor; }

    public String getResourceType() { return resourceType; }
    public String resourceType() { return resourceType; }

    public String getActionCode() { return actionCode; }
    public String actionCode() { return actionCode; }

    public LocalDateTime getFromTime() { return fromTime; }
    public LocalDateTime fromTime() { return fromTime; }
    public LocalDateTime getFromDate() { return fromTime; }
    public LocalDateTime fromDate() { return fromTime; }
    public LocalDateTime getTuNgay() { return fromTime; }
    public LocalDateTime tuNgay() { return fromTime; }

    public LocalDateTime getToTime() { return toTime; }
    public LocalDateTime toTime() { return toTime; }
    public LocalDateTime getToDate() { return toTime; }
    public LocalDateTime toDate() { return toTime; }
    public LocalDateTime getDenNgay() { return toTime; }
    public LocalDateTime denNgay() { return toTime; }

    public int getPage() { return page; }
    public int page() { return page; }

    public int getPageSize() { return pageSize; }
    public int pageSize() { return pageSize; }
}

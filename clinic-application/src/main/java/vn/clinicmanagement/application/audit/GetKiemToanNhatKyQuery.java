package vn.clinicmanagement.application.audit;

import vn.clinicmanagement.application.common.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Truy vấn tra cứu nhật ký kiểm toán (AUD-01).
 * Hỗ trợ lọc theo actor, resourceType, actionCode, khoảng thời gian và phân trang.
 * Yêu cầu thẩm quyền ADMIN hoặc AUDITOR.
 */
public class GetKiemToanNhatKyQuery implements Query<List<KiemToanNhatKyDuLieu>> {
    /** Trường `currentUserRole`: hientai người dùng vai trò. */
    private final String currentUserRole;
    /** Trường `actor`: tacnhan. */
    private final String actor;
    /** Trường `resourceType`: tainguyen loai. */
    private final String resourceType;
    /** Trường `actionCode`: hành động mã. */
    private final String actionCode;
    /** Trường `fromTime`: tu thời gian. */
    private final LocalDateTime fromTime;
    /** Trường `toTime`: den thời gian. */
    private final LocalDateTime toTime;
    private final int page;
    private final int pageSize;

    public GetKiemToanNhatKyQuery(String currentUserRole, String actor, String resourceType,
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

    public GetKiemToanNhatKyQuery(String currentUserRole, String actor, String resourceType,
                             String actionCode, LocalDateTime fromTime, LocalDateTime toTime) {
        this(currentUserRole, actor, resourceType, actionCode, fromTime, toTime, 0, 50);
    }

    public GetKiemToanNhatKyQuery(String currentUserRole) {
        this(currentUserRole, null, null, null, null, null, 0, 50);
    }

    public GetKiemToanNhatKyQuery(String actor, String resourceType, String actionCode,
                             LocalDateTime fromTime, LocalDateTime toTime) {
        this("ADMIN", actor, resourceType, actionCode, fromTime, toTime, 0, 50);
    }

    /** Hàm `getCurrentUserRole`: lấy hientai người dùng vai trò. */
    public String getCurrentUserRole() { return currentUserRole; }
    /** Hàm `currentUserRole`: hientai người dùng vai trò. */
    public String currentUserRole() { return currentUserRole; }
    /** Hàm `getActorRole`: lấy tacnhan vai trò. */
    public String getActorRole() { return currentUserRole; }
    /** Hàm `actorRole`: tacnhan vai trò. */
    public String actorRole() { return currentUserRole; }
    /** Hàm `getCallerRole`: lấy `Caller` vai trò. */
    public String getCallerRole() { return currentUserRole; }
    /** Hàm `callerRole`: `caller` vai trò. */
    public String callerRole() { return currentUserRole; }
    /** Hàm `getRole`: lấy vai trò. */
    public String getRole() { return currentUserRole; }
    /** Hàm `role`: vai trò. */
    public String role() { return currentUserRole; }

    /** Hàm `getActor`: lấy tacnhan. */
    public String getActor() { return actor; }
    /** Hàm `actor`: tacnhan. */
    public String actor() { return actor; }

    /** Hàm `getResourceType`: lấy tainguyen loai. */
    public String getResourceType() { return resourceType; }
    /** Hàm `resourceType`: tainguyen loai. */
    public String resourceType() { return resourceType; }

    /** Hàm `getActionCode`: lấy hành động mã. */
    public String getActionCode() { return actionCode; }
    /** Hàm `actionCode`: hành động mã. */
    public String actionCode() { return actionCode; }

    /** Hàm `getFromTime`: lấy tu thời gian. */
    public LocalDateTime getFromTime() { return fromTime; }
    /** Hàm `fromTime`: tu thời gian. */
    public LocalDateTime fromTime() { return fromTime; }
    /** Hàm `getFromDate`: lấy tu ngày. */
    public LocalDateTime getFromDate() { return fromTime; }
    /** Hàm `fromDate`: tu ngày. */
    public LocalDateTime fromDate() { return fromTime; }
    /** Hàm `getTuNgay`: lấy tu `Ngay`. */
    public LocalDateTime getTuNgay() { return fromTime; }
    public LocalDateTime tuNgay() { return fromTime; }

    /** Hàm `getToTime`: lấy den thời gian. */
    public LocalDateTime getToTime() { return toTime; }
    /** Hàm `toTime`: den thời gian. */
    public LocalDateTime toTime() { return toTime; }
    /** Hàm `getToDate`: lấy den ngày. */
    public LocalDateTime getToDate() { return toTime; }
    /** Hàm `toDate`: den ngày. */
    public LocalDateTime toDate() { return toTime; }
    /** Hàm `getDenNgay`: lấy `Den` `Ngay`. */
    public LocalDateTime getDenNgay() { return toTime; }
    public LocalDateTime denNgay() { return toTime; }

    /** Hàm `getPage`: lấy `Page`. */
    public int getPage() { return page; }
    public int page() { return page; }

    /** Hàm `getPageSize`: lấy `Page` `Size`. */
    public int getPageSize() { return pageSize; }
    public int pageSize() { return pageSize; }
}

package vn.clinicmanagement.application.audit;

import java.time.LocalDateTime;

/**
 * Đối tượng truyền dữ liệu (DTO) nhật ký kiểm toán (AUD-01).
 */
public class KiemToanNhatKyDuLieu {
    /** Trường `id`: mã. */
    private final Long id;
    /** Trường `actor`: tacnhan. */
    private final String actor;
    /** Trường `actorRole`: tacnhan vai trò. */
    private final String actorRole;
    /** Trường `resourceType`: tainguyen loai. */
    private final String resourceType;
    /** Trường `actionCode`: hành động mã. */
    private final String actionCode;
    /** Trường `resourceId`: tainguyen mã. */
    private final String resourceId;
    private final String details;
    private final String ipAddress;
    private final LocalDateTime timestamp;
    /** Trường `correlationId`: `correlation` mã. */
    private final String correlationId;

    public KiemToanNhatKyDuLieu(Long id, String actor, String actorRole, String resourceType,
                       String actionCode, String resourceId, String details,
                       String ipAddress, LocalDateTime timestamp, String correlationId) {
        this.id = id;
        this.actor = actor;
        this.actorRole = actorRole;
        this.resourceType = resourceType;
        this.actionCode = actionCode;
        this.resourceId = resourceId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.correlationId = correlationId;
    }

    public KiemToanNhatKyDuLieu(int id, String actor, String actorRole, String resourceType,
                       String actionCode, String resourceId, String details,
                       String ipAddress, LocalDateTime timestamp, String correlationId) {
        this((long) id, actor, actorRole, resourceType, actionCode, resourceId, details, ipAddress, timestamp, correlationId);
    }

    public KiemToanNhatKyDuLieu(Long id, String actor, String resourceType, String actionCode,
                       String details, LocalDateTime timestamp) {
        this(id, actor, null, resourceType, actionCode, null, details, null, timestamp, null);
    }

    public KiemToanNhatKyDuLieu(int id, String actor, String resourceType, String actionCode,
                       String details, LocalDateTime timestamp) {
        this((long) id, actor, null, resourceType, actionCode, null, details, null, timestamp, null);
    }

    /** Hàm `withMaskedSecrets`: voi `Masked` `Secrets`. */
    public KiemToanNhatKyDuLieu withMaskedSecrets() {
        return new KiemToanNhatKyDuLieu(
                id,
                actor,
                actorRole,
                resourceType,
                actionCode,
                resourceId,
                BiMatChe.mask(details),
                ipAddress,
                timestamp,
                correlationId
        );
    }

    /** Hàm `getId`: lấy mã. */
    public Long getId() { return id; }
    /** Hàm `id`: mã. */
    public Long id() { return id; }

    /** Hàm `getActor`: lấy tacnhan. */
    public String getActor() { return actor; }
    /** Hàm `actor`: tacnhan. */
    public String actor() { return actor; }

    /** Hàm `getActorRole`: lấy tacnhan vai trò. */
    public String getActorRole() { return actorRole; }
    /** Hàm `actorRole`: tacnhan vai trò. */
    public String actorRole() { return actorRole; }

    /** Hàm `getResourceType`: lấy tainguyen loai. */
    public String getResourceType() { return resourceType; }
    /** Hàm `resourceType`: tainguyen loai. */
    public String resourceType() { return resourceType; }

    /** Hàm `getActionCode`: lấy hành động mã. */
    public String getActionCode() { return actionCode; }
    /** Hàm `actionCode`: hành động mã. */
    public String actionCode() { return actionCode; }

    /** Hàm `getResourceId`: lấy tainguyen mã. */
    public String getResourceId() { return resourceId; }
    /** Hàm `resourceId`: tainguyen mã. */
    public String resourceId() { return resourceId; }

    /** Hàm `getDetails`: lấy `Details`. */
    public String getDetails() { return details; }
    public String details() { return details; }

    /** Hàm `getIpAddress`: lấy `Ip` `Address`. */
    public String getIpAddress() { return ipAddress; }
    public String ipAddress() { return ipAddress; }

    /** Hàm `getTimestamp`: lấy `Timestamp`. */
    public LocalDateTime getTimestamp() { return timestamp; }
    public LocalDateTime timestamp() { return timestamp; }
    /** Hàm `getThoiGian`: lấy `Thoi` `Gian`. */
    public LocalDateTime getThoiGian() { return timestamp; }
    public LocalDateTime thoiGian() { return timestamp; }

    /** Hàm `getCorrelationId`: lấy `Correlation` mã. */
    public String getCorrelationId() { return correlationId; }
    /** Hàm `correlationId`: `correlation` mã. */
    public String correlationId() { return correlationId; }
}

package vn.clinicmanagement.application.audit;

import java.time.LocalDateTime;

/**
 * Đối tượng truyền dữ liệu (DTO) nhật ký kiểm toán (AUD-01).
 */
public class AuditLogDto {
    private final Long id;
    private final String actor;
    private final String actorRole;
    private final String resourceType;
    private final String actionCode;
    private final String resourceId;
    private final String details;
    private final String ipAddress;
    private final LocalDateTime timestamp;
    private final String correlationId;

    public AuditLogDto(Long id, String actor, String actorRole, String resourceType,
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

    public AuditLogDto(int id, String actor, String actorRole, String resourceType,
                       String actionCode, String resourceId, String details,
                       String ipAddress, LocalDateTime timestamp, String correlationId) {
        this((long) id, actor, actorRole, resourceType, actionCode, resourceId, details, ipAddress, timestamp, correlationId);
    }

    public AuditLogDto(Long id, String actor, String resourceType, String actionCode,
                       String details, LocalDateTime timestamp) {
        this(id, actor, null, resourceType, actionCode, null, details, null, timestamp, null);
    }

    public AuditLogDto(int id, String actor, String resourceType, String actionCode,
                       String details, LocalDateTime timestamp) {
        this((long) id, actor, null, resourceType, actionCode, null, details, null, timestamp, null);
    }

    public AuditLogDto withMaskedSecrets() {
        return new AuditLogDto(
                id,
                actor,
                actorRole,
                resourceType,
                actionCode,
                resourceId,
                SecretMasker.mask(details),
                ipAddress,
                timestamp,
                correlationId
        );
    }

    public Long getId() { return id; }
    public Long id() { return id; }

    public String getActor() { return actor; }
    public String actor() { return actor; }

    public String getActorRole() { return actorRole; }
    public String actorRole() { return actorRole; }

    public String getResourceType() { return resourceType; }
    public String resourceType() { return resourceType; }

    public String getActionCode() { return actionCode; }
    public String actionCode() { return actionCode; }

    public String getResourceId() { return resourceId; }
    public String resourceId() { return resourceId; }

    public String getDetails() { return details; }
    public String details() { return details; }

    public String getIpAddress() { return ipAddress; }
    public String ipAddress() { return ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public LocalDateTime timestamp() { return timestamp; }
    public LocalDateTime getThoiGian() { return timestamp; }
    public LocalDateTime thoiGian() { return timestamp; }

    public String getCorrelationId() { return correlationId; }
    public String correlationId() { return correlationId; }
}

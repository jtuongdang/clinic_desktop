package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

final class AuditMetadataValidator {
    private static final Pattern ROLE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");
    private static final Pattern WORKSTATION_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");

    private AuditMetadataValidator() {
    }

    static void validate(
            Integer actorEmployeeId,
            Integer roleId,
            String roleCode,
            Integer facilityId,
            Integer departmentId,
            AuditActionCode actionCode,
            AuditResourceType resourceType,
            Integer resourceId,
            AuditOutcome outcome,
            Instant occurredAtUtc,
            String workstationId,
            UUID correlationId) {
        requirePositiveIfPresent(actorEmployeeId, "actorEmployeeId");
        requirePositiveIfPresent(roleId, "roleId");
        requirePositiveIfPresent(facilityId, "facilityId");
        requirePositiveIfPresent(departmentId, "departmentId");
        if ((roleId == null) != (roleCode == null)) {
            throw new IllegalArgumentException("Role ID and role code must both be present or both be absent.");
        }
        if (roleCode != null && !ROLE_CODE.matcher(roleCode).matches()) {
            throw new IllegalArgumentException("roleCode must be a normalized role code.");
        }
        if (actionCode == null) throw new IllegalArgumentException("actionCode is required.");
        if ((resourceType == null) != (resourceId == null)) {
            throw new IllegalArgumentException("Resource type and resource ID must both be present or absent.");
        }
        requirePositiveIfPresent(resourceId, "resourceId");
        if (outcome == null) throw new IllegalArgumentException("outcome is required.");
        if (occurredAtUtc == null) throw new IllegalArgumentException("occurredAtUtc is required.");
        if (workstationId != null && !WORKSTATION_ID.matcher(workstationId).matches()) {
            throw new IllegalArgumentException("workstationId must be a bounded identifier.");
        }
        if (correlationId == null) throw new IllegalArgumentException("correlationId is required.");
    }

    private static void requirePositiveIfPresent(Integer value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive when present.");
        }
    }
}

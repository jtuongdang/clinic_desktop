package vn.clinicmanagement.application.audit;

import java.util.UUID;
import java.util.regex.Pattern;

/** Validated, immutable actor and scope metadata for a single audited operation. */
public record KiemToanTacNhanNguCanh(
        /** Trường `employeeId`: nhân viên mã. */
        int employeeId,
        /** Trường `roleId`: vai trò mã. */
        int roleId,
        /** Trường `roleCode`: vai trò mã. */
        String roleCode,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `departmentId`: khoa mã. */
        int departmentId,
        /** Trường `workstationId`: `workstation` mã. */
        String workstationId,
        /** Trường `correlationId`: `correlation` mã. */
        UUID correlationId) {
    private static final Pattern ROLE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");
    private static final Pattern WORKSTATION_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");

    public KiemToanTacNhanNguCanh {
        if (employeeId <= 0) throw new IllegalArgumentException("employeeId must be positive.");
        if (roleId <= 0) throw new IllegalArgumentException("roleId must be positive.");
        if (roleCode == null || !ROLE_CODE.matcher(roleCode).matches()) {
            throw new IllegalArgumentException("roleCode must be a normalized role code.");
        }
        if (facilityId <= 0) throw new IllegalArgumentException("facilityId must be positive.");
        if (departmentId <= 0) throw new IllegalArgumentException("departmentId must be positive.");
        if (workstationId != null && workstationId.isBlank()) workstationId = null;
        if (workstationId != null && !WORKSTATION_ID.matcher(workstationId).matches()) {
            throw new IllegalArgumentException("workstationId must be a bounded identifier.");
        }
        if (correlationId == null) throw new IllegalArgumentException("correlationId is required.");
    }
}

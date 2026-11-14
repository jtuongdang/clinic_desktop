package vn.clinicmanagement.application.auth;

import java.util.List;
import java.util.Objects;

/** Authenticated identity and immutable assignment choices; no choice is implied. */
public record DaXacThucNguoiDung(int userId, String username, String fullName,
                                /** Trường `roles`: vai trò. */
                                /** Trường `workScopes`: làm việc `Scopes`. */
                                List<RoleChoice> roles, List<WorkScopeChoice> workScopes) {
    public DaXacThucNguoiDung {
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
        workScopes = List.copyOf(Objects.requireNonNull(workScopes, "workScopes"));
        if (roles.isEmpty() || workScopes.isEmpty()) {
            throw new IllegalArgumentException("At least one assigned role and work scope are required.");
        }
    }

    /** Trường `id`: mã. */
    /** Trường `code`: mã. */
    /** Trường `name`: tên. */
    public record RoleChoice(int id, String code, String name) {
        public RoleChoice {
            if (id <= 0) throw new IllegalArgumentException("Role ID must be positive.");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
        }

        /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
        @Override public String toString() { return name + " (" + code + ")"; }
    }

    /** Trường `facilityId`: cơ sở mã. */
    /** Trường `facilityName`: cơ sở tên. */
    /** Trường `departmentId`: khoa mã. */
    /** Trường `departmentName`: khoa tên. */
    public record WorkScopeChoice(int facilityId, String facilityName, int departmentId, String departmentName) {
        public WorkScopeChoice {
            if (facilityId <= 0 || departmentId <= 0) throw new IllegalArgumentException("Scope IDs must be positive.");
            Objects.requireNonNull(facilityName, "facilityName");
            Objects.requireNonNull(departmentName, "departmentName");
        }

        /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
        @Override public String toString() {
            return facilityName + " — " + departmentName;
        }
    }
}

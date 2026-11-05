package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.application.authorization.AuthorizationDecision.ReasonCode;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Default-deny application boundary for role, assignment, scope, and matrix checks. */
public final class AuthorizationService {
    private final ActiveSessionPort activeSessionPort;
    private final INhanVienRepository employeeRepository;
    private final ISystemRepository systemRepository;

    public AuthorizationService(ActiveSessionPort activeSessionPort,
                                INhanVienRepository employeeRepository,
                                ISystemRepository systemRepository) {
        this.activeSessionPort = Objects.requireNonNull(activeSessionPort);
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
        this.systemRepository = Objects.requireNonNull(systemRepository);
    }

    public AuthorizationDecision evaluate(AuthorizationRequest request) {
        if (request == null) return AuthorizationDecision.deny(ReasonCode.INVALID_REQUEST);
        AuthorizationContext context = request.context();
        if (context == null || !context.isValid()) {
            return AuthorizationDecision.deny(ReasonCode.INVALID_CONTEXT);
        }
        if (request.action() == null || request.targetFacilityId() <= 0 || request.targetDepartmentId() <= 0) {
            return AuthorizationDecision.deny(ReasonCode.INVALID_REQUEST);
        }
        if (request.action() == HanhDongQuyen.AUDIT_READ) {
            return AuthorizationDecision.deny(ReasonCode.AUDIT_READ_DENIED);
        }
        String module = normalize(request.moduleCode());
        if (module == null) return AuthorizationDecision.deny(ReasonCode.MODULE_INVALID);
        if (request.targetFacilityId() != context.facilityId()
                || request.targetDepartmentId() != context.departmentId()) {
            return AuthorizationDecision.deny(ReasonCode.TARGET_MISMATCH);
        }

        try {
            Optional<ActiveSessionPort.ActiveSession> sessionResult = activeSessionPort.findActiveSession(context.sessionId());
            if (sessionResult == null || sessionResult.isEmpty()) {
                return AuthorizationDecision.deny(ReasonCode.SESSION_INVALID);
            }
            ActiveSessionPort.ActiveSession session = sessionResult.get();
            if (!session.active() || !session.unlocked() || !session.notExpired()
                    || !Objects.equals(session.sessionId(), context.sessionId())
                    || session.employeeId() != context.employeeId()
                    || session.roleId() != context.roleId()
                    || !sameCode(session.roleCode(), context.roleCode())
                    || session.facilityId() != context.facilityId()
                    || session.departmentId() != context.departmentId()) {
                return AuthorizationDecision.deny(ReasonCode.SESSION_INVALID);
            }

            Optional<NhanVien> employeeResult = employeeRepository.getById(context.employeeId());
            if (employeeResult == null || employeeResult.isEmpty()) {
                return AuthorizationDecision.deny(ReasonCode.EMPLOYEE_INVALID);
            }
            NhanVien employee = employeeResult.get();
            if (!employee.isHoatDong() || employee.isDeleted()
                    || employee.getId() == null || employee.getId() != context.employeeId()) {
                return AuthorizationDecision.deny(ReasonCode.EMPLOYEE_INVALID);
            }

            List<VaiTro> roles = employeeRepository.getAssignedRoles(context.employeeId());
            if (roles == null) return AuthorizationDecision.deny(ReasonCode.DEPENDENCY_FAILURE);
            long roleMatches = roles.stream().filter(role -> role != null && !role.isDeleted()
                    && role.getId() != null && role.getId() == context.roleId()
                    && sameCode(role.getTenVaiTro(), context.roleCode())).count();
            if (roleMatches != 1) return AuthorizationDecision.deny(ReasonCode.ROLE_INVALID);

            List<NhanVienWorkScope> scopes = employeeRepository.getAssignedWorkScopes(context.employeeId());
            if (scopes == null) return AuthorizationDecision.deny(ReasonCode.DEPENDENCY_FAILURE);
            long scopeMatches = scopes.stream().filter(scope -> scope != null
                    && scope.maCoSo() == context.facilityId()
                    && scope.maKhoa() == context.departmentId()).count();
            if (scopeMatches != 1) return AuthorizationDecision.deny(ReasonCode.SCOPE_INVALID);

            List<MaTranPhanQuyen> matrix = systemRepository.getMatrixByRoleId(context.roleId());
            if (matrix == null) return AuthorizationDecision.deny(ReasonCode.DEPENDENCY_FAILURE);
            List<MaTranPhanQuyen> matches = matrix.stream().filter(permission -> permission != null
                    && !permission.isDeleted()
                    && permission.getMaVaiTro() == context.roleId()
                    && request.action() == permission.getHanhDong()
                    && module.equals(normalize(permission.getPhanHe()))).toList();
            if (matches.isEmpty()) return AuthorizationDecision.deny(ReasonCode.PERMISSION_MISSING);
            if (matches.size() != 1) return AuthorizationDecision.deny(ReasonCode.PERMISSION_AMBIGUOUS);
            MaTranPhanQuyen grant = matches.get(0);
            if (!grant.isDuocPhep()) return AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED);
            if (grant.getPhamViDuLieu() != PhamViDuLieu.KHOA_PHONG) {
                return AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED);
            }
            return AuthorizationDecision.allow();
        } catch (RuntimeException ignored) {
            return AuthorizationDecision.deny(ReasonCode.DEPENDENCY_FAILURE);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean sameCode(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }
}

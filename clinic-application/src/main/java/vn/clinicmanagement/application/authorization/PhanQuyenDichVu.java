package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh.ReasonCode;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Default-deny application boundary for role, assignment, scope, and matrix checks. */
public final class PhanQuyenDichVu {
    /** Trường `activeSessionPort`: hoạt động phien congketnoi. */
    private final HoatDongPhienCongKetNoi activeSessionPort;
    /** Trường `employeeRepository`: nhân viên kho dữ liệu. */
    private final INhanVienRepository employeeRepository;
    /** Trường `systemRepository`: hệ thống kho dữ liệu. */
    private final IHeThongRepository systemRepository;

    public PhanQuyenDichVu(HoatDongPhienCongKetNoi activeSessionPort,
                                INhanVienRepository employeeRepository,
                                IHeThongRepository systemRepository) {
        this.activeSessionPort = Objects.requireNonNull(activeSessionPort);
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
        this.systemRepository = Objects.requireNonNull(systemRepository);
    }

    public PhanQuyenQuyetDinh evaluate(PhanQuyenYeuCau request) {
        if (request == null) return PhanQuyenQuyetDinh.deny(ReasonCode.INVALID_REQUEST);
        PhanQuyenNguCanh context = request.context();
        if (context == null || !context.isValid()) {
            return PhanQuyenQuyetDinh.deny(ReasonCode.INVALID_CONTEXT);
        }
        if (!DaDuyetVaiTroMa.isAllowed(context.roleCode())) {
            return PhanQuyenQuyetDinh.deny(ReasonCode.ROLE_INVALID);
        }
        if (request.action() == null || request.targetFacilityId() <= 0 || request.targetDepartmentId() <= 0) {
            return PhanQuyenQuyetDinh.deny(ReasonCode.INVALID_REQUEST);
        }
        if (request.action() == HanhDongQuyen.AUDIT_READ) {
            return PhanQuyenQuyetDinh.deny(ReasonCode.AUDIT_READ_DENIED);
        }
        String module = normalize(request.moduleCode());
        if (module == null) return PhanQuyenQuyetDinh.deny(ReasonCode.MODULE_INVALID);
        if (request.targetFacilityId() != context.facilityId()
                || request.targetDepartmentId() != context.departmentId()) {
            return PhanQuyenQuyetDinh.deny(ReasonCode.TARGET_MISMATCH);
        }

        try {
            Optional<HoatDongPhienCongKetNoi.ActiveSession> sessionResult = activeSessionPort.findActiveSession(context.sessionId());
            if (sessionResult == null || sessionResult.isEmpty()) {
                return PhanQuyenQuyetDinh.deny(ReasonCode.SESSION_INVALID);
            }
            HoatDongPhienCongKetNoi.ActiveSession session = sessionResult.get();
            if (!session.active() || !session.unlocked() || !session.notExpired()
                    || !Objects.equals(session.sessionId(), context.sessionId())
                    || session.employeeId() != context.employeeId()
                    || session.roleId() != context.roleId()
                    || !sameCode(session.roleCode(), context.roleCode())
                    || session.facilityId() != context.facilityId()
                    || session.departmentId() != context.departmentId()) {
                return PhanQuyenQuyetDinh.deny(ReasonCode.SESSION_INVALID);
            }

            Optional<NhanVien> employeeResult = employeeRepository.getById(context.employeeId());
            if (employeeResult == null || employeeResult.isEmpty()) {
                return PhanQuyenQuyetDinh.deny(ReasonCode.EMPLOYEE_INVALID);
            }
            NhanVien employee = employeeResult.get();
            if (!employee.isHoatDong() || employee.isDeleted()
                    || employee.getId() == null || employee.getId() != context.employeeId()) {
                return PhanQuyenQuyetDinh.deny(ReasonCode.EMPLOYEE_INVALID);
            }

            List<VaiTro> roles = employeeRepository.getAssignedRoles(context.employeeId());
            if (roles == null) return PhanQuyenQuyetDinh.deny(ReasonCode.DEPENDENCY_FAILURE);
            long roleMatches = roles.stream().filter(role -> role != null && !role.isDeleted()
                    && role.getId() != null && role.getId() == context.roleId()
                    && sameCode(role.getTenVaiTro(), context.roleCode())).count();
            if (roleMatches != 1) return PhanQuyenQuyetDinh.deny(ReasonCode.ROLE_INVALID);

            List<NhanVienWorkPhamVi> scopes = employeeRepository.getAssignedWorkScopes(context.employeeId());
            if (scopes == null) return PhanQuyenQuyetDinh.deny(ReasonCode.DEPENDENCY_FAILURE);
            long scopeMatches = scopes.stream().filter(scope -> scope != null
                    && scope.maCoSo() == context.facilityId()
                    && scope.maKhoa() == context.departmentId()).count();
            if (scopeMatches != 1) return PhanQuyenQuyetDinh.deny(ReasonCode.SCOPE_INVALID);

            List<MaTranPhanQuyen> matrix = systemRepository.getMatrixByRoleId(context.roleId());
            if (matrix == null) return PhanQuyenQuyetDinh.deny(ReasonCode.DEPENDENCY_FAILURE);
            List<MaTranPhanQuyen> matches = matrix.stream().filter(permission -> permission != null
                    && !permission.isDeleted()
                    && permission.getMaVaiTro() == context.roleId()
                    && request.action() == permission.getHanhDong()
                    && module.equals(normalize(permission.getPhanHe()))).toList();
            if (matches.isEmpty()) return PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_MISSING);
            if (matches.size() != 1) return PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_AMBIGUOUS);
            MaTranPhanQuyen grant = matches.get(0);
            if (!grant.isDuocPhep()) return PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED);
            if (grant.getPhamViDuLieu() != PhamViDuLieu.KHOA_PHONG) {
                return PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED);
            }
            return PhanQuyenQuyetDinh.allow();
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure(
                    "AUTHORIZATION_EVALUATE", "DEPENDENCY_FAILURE", UUID.randomUUID().toString(), ex);
            return PhanQuyenQuyetDinh.deny(ReasonCode.DEPENDENCY_FAILURE);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Hàm `sameCode`: `same` mã. */
    private static boolean sameCode(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }
}

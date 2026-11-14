package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Loads only active, approved, role-scoped XEM grants for desktop navigation. */
public final class DieuHuongQuyenHandler {
    private static final Set<String> APPROVED_MODULE_CODES = Set.of(
            "HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "EMR", "CLS",
            "PRESCRIPTION", "INVENTORY", "BILLING", "CATALOG", "SYSTEM");

    /** Trường `systemRepository`: hệ thống kho dữ liệu. */
    private final IHeThongRepository systemRepository;

    public DieuHuongQuyenHandler(IHeThongRepository systemRepository) {
        this.systemRepository = Objects.requireNonNull(systemRepository, "systemRepository");
    }

    public KetQua<Set<String>> handle(int roleId, String roleCode) {
        if (roleId <= 0 || !DaDuyetVaiTroMa.isAllowed(roleCode)) {
            return KetQua.failure("NAVIGATION_ROLE_INVALID", "Không thể xác định vai trò điều hướng.");
        }

        final List<MaTranPhanQuyen> permissions;
        try {
            permissions = systemRepository.getMatrixByRoleId(roleId);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure(
                    "NAVIGATION_LOAD_PERMISSIONS", "NAVIGATION_PERMISSIONS_UNAVAILABLE",
                    UUID.randomUUID().toString(), ex);
            return KetQua.failure("NAVIGATION_PERMISSIONS_UNAVAILABLE", "Không thể tải quyền điều hướng.");
        }
        if (permissions == null) {
            return KetQua.failure("NAVIGATION_PERMISSIONS_UNAVAILABLE", "Không thể tải quyền điều hướng.");
        }

        List<MaTranPhanQuyen> activeViewRows = permissions.stream()
                .filter(Objects::nonNull)
                .filter(permission -> !permission.isDeleted())
                .filter(permission -> permission.getMaVaiTro() == roleId)
                .filter(permission -> permission.getHanhDong() == HanhDongQuyen.XEM)
                .filter(permission -> {
                    String moduleCode = normalize(permission.getPhanHe());
                    return moduleCode != null && APPROVED_MODULE_CODES.contains(moduleCode);
                })
                .toList();

        List<String> moduleCodes = activeViewRows.stream()
                .map(permission -> normalize(permission.getPhanHe()))
                .distinct()
                .toList();
        Set<String> grantedModules = new java.util.LinkedHashSet<>();
        for (String moduleCode : moduleCodes) {
            List<MaTranPhanQuyen> matchingRows = new ArrayList<>();
            for (MaTranPhanQuyen permission : activeViewRows) {
                if (moduleCode.equals(normalize(permission.getPhanHe()))) matchingRows.add(permission);
            }
            if (matchingRows.size() != 1) continue;

            MaTranPhanQuyen grant = matchingRows.get(0);
            if (grant.isDuocPhep() && grant.getPhamViDuLieu() == PhamViDuLieu.KHOA_PHONG) {
                grantedModules.add(moduleCode);
            }
        }
        if (grantedModules.isEmpty()) {
            return KetQua.failure("NAVIGATION_GRANTS_MISSING", "Vai trò hiện tại không có phân hệ được phép xem.");
        }
        return KetQua.success(Set.copyOf(grantedModules));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}

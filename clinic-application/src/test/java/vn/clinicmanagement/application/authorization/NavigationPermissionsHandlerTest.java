package vn.clinicmanagement.application.authorization;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NavigationPermissionsHandlerTest {
    private static final int ROLE_ID = 17;

    @Test
    void returnsApprovedXemModulesForEveryRole() {
        assertModules("ADMIN", "HOME", "CATALOG", "SYSTEM");
        assertModules("DOCTOR", "HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "EMR",
                "CLS", "PRESCRIPTION", "INVENTORY", "BILLING", "CATALOG");
        assertModules("RECEPTIONIST", "HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "CATALOG");
        assertModules("PHARMACIST", "HOME", "PATIENT", "PRESCRIPTION", "INVENTORY", "CATALOG");
        assertModules("CASHIER", "HOME", "PATIENT", "APPOINTMENT", "CLS", "BILLING", "CATALOG");
        assertModules("LAB_TECH", "HOME", "PATIENT", "CLS", "CATALOG");
    }

    @Test
    void rejectsMissingOrUnknownRoleWithoutLoadingPermissions() {
        ISystemRepository repository = repository(null);
        NavigationPermissionsHandler handler = new NavigationPermissionsHandler(repository);

        assertTrue(handler.handle(0, "ADMIN").isFailure());
        assertTrue(handler.handle(ROLE_ID, null).isFailure());
        assertTrue(handler.handle(ROLE_ID, "Quản trị viên toàn hệ thống").isFailure());
        assertTrue(handler.handle(ROLE_ID, "UNKNOWN").isFailure());
    }

    @Test
    void failsClosedForMissingUnknownInactiveDeniedOrOutOfScopeGrants() {
        assertTrue(new NavigationPermissionsHandler(repository(List.of()))
                .handle(ROLE_ID, "ADMIN").isFailure());
        assertTrue(new NavigationPermissionsHandler(repository(List.of(
                permission(ROLE_ID, "LEGACY_MODULE", HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG, false))))
                .handle(ROLE_ID, "ADMIN").isFailure());

        List<MaTranPhanQuyen> rows = new ArrayList<>(List.of(
                permission(ROLE_ID, "HOME", HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG, false),
                permission(ROLE_ID, "CATALOG", HanhDongQuyen.XEM, false, PhamViDuLieu.KHOA_PHONG, false),
                permission(ROLE_ID, "SYSTEM", HanhDongQuyen.XEM, true, PhamViDuLieu.TOAN_HE_THONG, false),
                permission(ROLE_ID, "RECEPTION", HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG, true),
                permission(ROLE_ID, "BILLING", HanhDongQuyen.TAO, true, PhamViDuLieu.KHOA_PHONG, false)));

        Result<Set<String>> result = new NavigationPermissionsHandler(repository(rows)).handle(ROLE_ID, "ADMIN");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("HOME"), result.valueOrThrow());
    }

    @Test
    void failsClosedForRepositoryErrorsNullDataAndAmbiguousRows() {
        ISystemRepository failingRepository = new ISystemRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                throw new IllegalStateException("synthetic repository failure");
            }
            @Override public java.util.Optional<String> getSystemParameterValue(String key) {
                return java.util.Optional.empty();
            }
        };
        assertTrue(new NavigationPermissionsHandler(failingRepository).handle(ROLE_ID, "ADMIN").isFailure());
        assertTrue(new NavigationPermissionsHandler(repository(null)).handle(ROLE_ID, "ADMIN").isFailure());

        List<MaTranPhanQuyen> duplicateRows = List.of(
                permission(ROLE_ID, "HOME", HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG, false),
                permission(ROLE_ID, "HOME", HanhDongQuyen.XEM, false, PhamViDuLieu.KHOA_PHONG, false));
        Result<Set<String>> result = new NavigationPermissionsHandler(repository(duplicateRows))
                .handle(ROLE_ID, "ADMIN");
        assertTrue(result.isFailure());
    }

    private static void assertModules(String roleCode, String... modules) {
        List<MaTranPhanQuyen> rows = new ArrayList<>();
        for (String module : modules) {
            rows.add(permission(ROLE_ID, module, HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG, false));
        }
        Result<Set<String>> result = new NavigationPermissionsHandler(repository(rows)).handle(ROLE_ID, roleCode);
        assertTrue(result.isSuccess(), roleCode);
        assertEquals(Set.of(modules), result.valueOrThrow(), roleCode);
    }

    private static ISystemRepository repository(List<MaTranPhanQuyen> rows) {
        return new ISystemRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return rows; }
            @Override public java.util.Optional<String> getSystemParameterValue(String key) {
                return java.util.Optional.empty();
            }
        };
    }

    private static MaTranPhanQuyen permission(
            int roleId, String module, HanhDongQuyen action, boolean allowed,
            PhamViDuLieu scope, boolean deleted) {
        MaTranPhanQuyen permission = new MaTranPhanQuyen();
        permission.setMaVaiTro(roleId);
        permission.setPhanHe(module);
        permission.setHanhDong(action);
        permission.setDuocPhep(allowed);
        permission.setPhamViDuLieu(scope);
        permission.setDeleted(deleted);
        return permission;
    }
}

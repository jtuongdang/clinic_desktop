package vn.clinicmanagement.application.authorization;

import org.junit.jupiter.api.Test;
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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PhanQuyenDichVuTest {
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final String ROLE_CODE = "DOCTOR";
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final String MODULE = "RECEPTION";
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");

    @Test
    void allowsOnlyCurrentRoleGrantForExactAssignedTargetPair() {
        Fixture fixture = new Fixture();
        PhanQuyenQuyetDinh decision = fixture.evaluate(fixture.request(MODULE.toLowerCase(), HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID));

        assertTrue(decision.allowed());
        assertEquals(ReasonCode.ALLOWED, decision.reasonCode());
    }

    @Test
    void allowsEachApprovedRoleThroughTheFullAuthorizationFlow() {
        for (String roleCode : List.of("ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH")) {
            Fixture fixture = new Fixture();
            fixture.useRoleCode(roleCode.equals("DOCTOR") ? " doctor " : roleCode);

            PhanQuyenQuyetDinh decision = fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID));

            assertTrue(decision.allowed(), roleCode);
            assertEquals(ReasonCode.ALLOWED, decision.reasonCode(), roleCode);
            assertEquals(1, fixture.matrixLookups, roleCode);
        }
    }

    @Test
    void deniesUnsupportedRoleCodesBeforeLoadingTheirMatchingGrant() {
        for (String roleCode : List.of("NURSE", "HEAD_DOCTOR", "CLINICIAN")) {
            Fixture fixture = new Fixture();
            fixture.useRoleCode(roleCode);

            assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID)), ReasonCode.ROLE_INVALID);
            assertEquals(0, fixture.matrixLookups, roleCode);
        }
    }

    @Test
    void deniesNullOrInvalidRequestsAndContexts() {
        Fixture fixture = new Fixture();
        assertReason(fixture.evaluate(null), ReasonCode.INVALID_REQUEST);
        assertReason(fixture.evaluate(new PhanQuyenYeuCau(null, MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.INVALID_CONTEXT);
        assertReason(fixture.evaluate(fixture.request("  ", HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.MODULE_INVALID);
        assertReason(fixture.evaluate(fixture.request("UNKNOWN_MODULE", HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_MISSING);
        assertReason(fixture.evaluate(fixture.request(MODULE, null, FACILITY_ID, DEPARTMENT_ID)),
                ReasonCode.INVALID_REQUEST);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM, 0, DEPARTMENT_ID)),
                ReasonCode.INVALID_REQUEST);
        PhanQuyenNguCanh invalidActor = new PhanQuyenNguCanh(0, ROLE_ID, ROLE_CODE,
                FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        assertReason(fixture.evaluate(new PhanQuyenYeuCau(invalidActor, MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.INVALID_CONTEXT);
    }

    @Test
    void deniesAuditReadEvenForAdminWithAnAllowRow() {
        Fixture fixture = new Fixture();
        fixture.context = context(ROLE_ID, "ADMIN", FACILITY_ID, DEPARTMENT_ID);
        fixture.roles = List.of(role(ROLE_ID, "ADMIN"));
        fixture.session = activeSession(fixture.context);
        fixture.grants = List.of(grant(ROLE_ID, MODULE, HanhDongQuyen.AUDIT_READ, true,
                PhamViDuLieu.KHOA_PHONG));

        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.AUDIT_READ,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.AUDIT_READ_DENIED);
    }

    @Test
    void deniesTargetInDifferentDepartmentOrFacility() {
        Fixture fixture = new Fixture();
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID + 1)), ReasonCode.TARGET_MISMATCH);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID + 1, DEPARTMENT_ID)), ReasonCode.TARGET_MISMATCH);
    }

    @Test
    void deniesEveryInactiveOrMismatchedSessionState() {
        for (int state = 0; state < 9; state++) {
            Fixture fixture = new Fixture();
            fixture.session = switch (state) {
                case 0 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, false, true, true);
                case 1 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, true, false, true);
                case 2 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, true, true, false);
                case 3 -> new HoatDongPhienCongKetNoi.ActiveSession(UUID.randomUUID(), EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, true, true, true);
                case 4 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID + 1, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, true, true, true);
                case 5 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID + 1, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID, true, true, true);
                case 6 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, "OTHER",
                        FACILITY_ID, DEPARTMENT_ID, true, true, true);
                case 7 -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID + 1, DEPARTMENT_ID, true, true, true);
                default -> new HoatDongPhienCongKetNoi.ActiveSession(SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE,
                        FACILITY_ID, DEPARTMENT_ID + 1, true, true, true);
            };
            assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SESSION_INVALID);
        }
        Fixture missing = new Fixture();
        missing.session = null;
        assertReason(missing.evaluate(missing.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SESSION_INVALID);
    }

    @Test
    void deniesInactiveOrDeletedEmployee() {
        Fixture fixture = new Fixture();
        fixture.employee.setHoatDong(false);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.EMPLOYEE_INVALID);
        fixture = new Fixture();
        fixture.employee.setDeleted(true);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.EMPLOYEE_INVALID);
    }

    @Test
    void deniesRevokedOrMismatchedRoleAssignment() {
        Fixture fixture = new Fixture();
        fixture.roles = List.of();
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.ROLE_INVALID);
        fixture = new Fixture();
        fixture.roles = List.of(role(ROLE_ID, "OTHER"));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.ROLE_INVALID);
        fixture = new Fixture();
        fixture.roles = List.of(role(ROLE_ID, ROLE_CODE), role(ROLE_ID, ROLE_CODE));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.ROLE_INVALID);
        fixture = new Fixture();
        VaiTro revokedRole = role(ROLE_ID, ROLE_CODE);
        revokedRole.setDeleted(true);
        fixture.roles = List.of(revokedRole);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.ROLE_INVALID);
    }

    @Test
    void deniesRevokedOrWrongAssignedFacilityDepartmentPair() {
        Fixture fixture = new Fixture();
        fixture.scopes = List.of();
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SCOPE_INVALID);
        fixture = new Fixture();
        fixture.scopes = List.of(new NhanVienWorkPhamVi(FACILITY_ID, "Synthetic Facility",
                DEPARTMENT_ID + 1, "Synthetic Department"));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SCOPE_INVALID);
        fixture = new Fixture();
        fixture.scopes = List.of(new NhanVienWorkPhamVi(FACILITY_ID + 1, "Synthetic Facility",
                DEPARTMENT_ID, "Synthetic Department"));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SCOPE_INVALID);
        fixture = new Fixture();
        fixture.scopes = List.of(
                new NhanVienWorkPhamVi(FACILITY_ID, "Synthetic Facility", DEPARTMENT_ID, "Synthetic Department"),
                new NhanVienWorkPhamVi(FACILITY_ID, "Synthetic Facility", DEPARTMENT_ID, "Synthetic Department"));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.SCOPE_INVALID);
    }

    @Test
    void deniesMissingFalseAndDuplicatePermissionRows() {
        Fixture fixture = new Fixture();
        fixture.grants = List.of();
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_MISSING);
        fixture = new Fixture();
        fixture.grants = List.of(grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, false,
                PhamViDuLieu.KHOA_PHONG));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_DENIED);
        fixture = new Fixture();
        fixture.grants = List.of(grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, true,
                PhamViDuLieu.KHOA_PHONG), grant(ROLE_ID, MODULE.toLowerCase(), HanhDongQuyen.XEM, true,
                PhamViDuLieu.KHOA_PHONG));
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_AMBIGUOUS);
    }

    @Test
    void ignoresRevokedPermissionRowsWhenCountingActiveGrants() {
        Fixture fixture = new Fixture();
        MaTranPhanQuyen revoked = grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, true,
                PhamViDuLieu.KHOA_PHONG);
        revoked.setDeleted(true);
        fixture.grants = List.of(revoked);
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_MISSING);

        fixture.grants = List.of(revoked, grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, true,
                PhamViDuLieu.KHOA_PHONG));
        assertTrue(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)).allowed());
    }

    @Test
    void deniesAllLegacyWidePermissionScopes() {
        for (PhamViDuLieu scope : List.of(PhamViDuLieu.TOAN_HE_THONG,
                PhamViDuLieu.CO_SO, PhamViDuLieu.CHI_NHANH)) {
            Fixture fixture = new Fixture();
            fixture.grants = List.of(grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, true, scope));
            assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_DENIED);
        }
    }

    @Test
    void convertsSessionAndRepositoryFailuresToSafeDenials() {
        Fixture fixture = new Fixture();
        fixture.sessionFailure = true;
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);
        fixture = new Fixture();
        fixture.employeeFailure = true;
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);
        fixture = new Fixture();
        fixture.rolesFailure = true;
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);
        fixture = new Fixture();
        fixture.scopesFailure = true;
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);
        fixture = new Fixture();
        fixture.matrixFailure = true;
        assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);
    }

    @Test
    void unexpectedRepositoryFailureLogsSafeDiagnosticsAndKeepsDefaultDeny() {
        Fixture fixture = new Fixture();
        fixture.matrixFailure = true;

        try (CapturedDiagnostics diagnostics = new CapturedDiagnostics()) {
            assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID)), ReasonCode.DEPENDENCY_FAILURE);

            assertEquals(1, diagnostics.messages.size());
            String message = diagnostics.messages.get(0);
            assertTrue(message.contains("operation=AUTHORIZATION_EVALUATE"));
            assertTrue(message.contains("result=DEPENDENCY_FAILURE"));
            assertTrue(message.contains("correlationId="));
            String correlationId = message.substring(message.indexOf("correlationId=") + "correlationId=".length(),
                    message.indexOf(' ', message.indexOf("correlationId=")));
            assertDoesNotThrow(() -> UUID.fromString(correlationId));
            assertTrue(message.contains("exceptions=[java.lang.IllegalStateException]"));
            assertTrue(message.contains("AuthorizationService#evaluate:"));
            String frames = message.substring(message.indexOf("frames=[") + "frames=[".length(),
                    message.lastIndexOf(']'));
            assertTrue(frames.isEmpty() || frames.split(",").length <= 8);
            assertFalse(message.contains("synthetic matrix failure"));
            assertFalse(message.contains("IllegalStateException: synthetic"));
        }
    }

    @Test
    void ordinaryAuthorizationDenialDoesNotLogUnexpectedDiagnostics() {
        Fixture fixture = new Fixture();
        fixture.grants = List.of(grant(ROLE_ID, MODULE, HanhDongQuyen.XEM, false,
                PhamViDuLieu.KHOA_PHONG));

        try (CapturedDiagnostics diagnostics = new CapturedDiagnostics()) {
            assertReason(fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                    FACILITY_ID, DEPARTMENT_ID)), ReasonCode.PERMISSION_DENIED);
            assertTrue(diagnostics.messages.isEmpty());
        }
    }

    private static final class CapturedDiagnostics implements AutoCloseable {
        private final Logger logger = Logger.getLogger(UngDungThatBaiChanDoanLoi.class.getName());
        private final List<String> messages = new ArrayList<>();
        private final Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };

        private CapturedDiagnostics() { logger.addHandler(handler); }

        @Override public void close() { logger.removeHandler(handler); }
    }

    @Test
    void safeOutputsDoNotRenderActorOrSessionIdentifiers() {
        Fixture fixture = new Fixture();
        String contextText = fixture.context.toString();
        String requestText = fixture.request(MODULE, HanhDongQuyen.XEM, FACILITY_ID, DEPARTMENT_ID).toString();
        String sessionText = activeSession(fixture.context).toString();
        String decisionText = fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)).toString();
        for (String text : List.of(contextText, requestText, sessionText, decisionText)) {
            assertFalse(text.contains(Integer.toString(EMPLOYEE_ID)));
            assertFalse(text.contains(SESSION_ID.toString()));
            assertFalse(text.contains(ROLE_CODE));
        }
        assertEquals(ReasonCode.ALLOWED, fixture.evaluate(fixture.request(MODULE, HanhDongQuyen.XEM,
                FACILITY_ID, DEPARTMENT_ID)).reasonCode());
    }

    private static void assertReason(PhanQuyenQuyetDinh decision, ReasonCode expected) {
        assertFalse(decision.allowed());
        assertEquals(expected, decision.reasonCode());
    }

    private static VaiTro role(int id, String code) {
        VaiTro role = new VaiTro();
        role.setId(id);
        role.setTenVaiTro(code);
        return role;
    }

    private static MaTranPhanQuyen grant(int roleId, String module, HanhDongQuyen action,
                                         boolean allowed, PhamViDuLieu scope) {
        MaTranPhanQuyen grant = new MaTranPhanQuyen();
        grant.setMaVaiTro(roleId);
        grant.setPhanHe(module);
        grant.setHanhDong(action);
        grant.setDuocPhep(allowed);
        grant.setPhamViDuLieu(scope);
        return grant;
    }

    private static HoatDongPhienCongKetNoi.ActiveSession activeSession(PhanQuyenNguCanh context) {
        return new HoatDongPhienCongKetNoi.ActiveSession(context.sessionId(), context.employeeId(), context.roleId(),
                context.roleCode(), context.facilityId(), context.departmentId(), true, true, true);
    }

    private static final class Fixture {
        private PhanQuyenNguCanh context = context(ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID);
        private final NhanVien employee = new NhanVien();
        private List<VaiTro> roles = List.of(role(ROLE_ID, ROLE_CODE));
        private List<NhanVienWorkPhamVi> scopes = List.of(new NhanVienWorkPhamVi(FACILITY_ID,
                "Synthetic Facility", DEPARTMENT_ID, "Synthetic Department"));
        private List<MaTranPhanQuyen> grants = List.of(grant(ROLE_ID, MODULE,
                HanhDongQuyen.XEM, true, PhamViDuLieu.KHOA_PHONG));
        private HoatDongPhienCongKetNoi.ActiveSession session = activeSession(context);
        private boolean sessionFailure;
        private boolean employeeFailure;
        private boolean rolesFailure;
        private boolean scopesFailure;
        private boolean matrixFailure;
        private int matrixLookups;
        private final PhanQuyenDichVu service;

        private Fixture() {
            employee.setId(EMPLOYEE_ID);
            HoatDongPhienCongKetNoi sessionPort = sessionId -> {
                if (sessionFailure) throw new IllegalStateException("synthetic session failure");
                return Optional.ofNullable(session);
            };
            INhanVienRepository employeePort = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> {
                            if (employeeFailure) throw new IllegalStateException("synthetic employee failure");
                            yield Optional.of(employee);
                        }
                        case "getAssignedRoles" -> {
                            if (rolesFailure) throw new IllegalStateException("synthetic role failure");
                            yield roles;
                        }
                        case "getAssignedWorkScopes" -> {
                            if (scopesFailure) throw new IllegalStateException("synthetic scope failure");
                            yield scopes;
                        }
                        case "toString" -> "SyntheticEmployeeRepository";
                        default -> null;
                    });
            IHeThongRepository systemPort = new IHeThongRepository() {
                @Override
                public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                    matrixLookups++;
                    if (matrixFailure) throw new IllegalStateException("synthetic matrix failure");
                    return grants;
                }

                @Override
                public Optional<String> getSystemParameterValue(String key) {
                    return Optional.empty();
                }
            };
            service = new PhanQuyenDichVu(sessionPort, employeePort, systemPort);
        }

        private PhanQuyenQuyetDinh evaluate(PhanQuyenYeuCau request) {
            return service.evaluate(request);
        }

        private PhanQuyenYeuCau request(String module, HanhDongQuyen action, int facility, int department) {
            return new PhanQuyenYeuCau(context, module, action, facility, department);
        }

        private void useRoleCode(String roleCode) {
            context = context(ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID);
            roles = List.of(role(ROLE_ID, roleCode));
            session = activeSession(context);
        }
    }

    private static PhanQuyenNguCanh context(int roleId, String roleCode, int facility, int department) {
        return new PhanQuyenNguCanh(EMPLOYEE_ID, roleId, roleCode, facility, department, SESSION_ID);
    }
}

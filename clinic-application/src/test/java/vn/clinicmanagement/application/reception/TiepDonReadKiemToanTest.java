package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh.ReasonCode;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonReadKiemToanTest {

    private static final int EMPLOYEE_ID = 42;
    private static final int ROLE_ID = 5;
    private static final String ROLE_CODE = "RECEPTIONIST";
    private static final int FACILITY_ID = 2;
    private static final int DEPARTMENT_ID = 7;
    private static final String WORKSTATION_ID = "DESK_01";
    private static final String MODULE = "RECEPTION";
    private static final UUID SESSION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID CORRELATION_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");

    private TestAuditWritePort auditPort;
    private KiemToanTacNhanNguCanh validActor;
    private PhanQuyenNguCanh validContext;

    @BeforeEach
    void setUp() {
        auditPort = new TestAuditWritePort();
        validActor = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID
        );
        validContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
        );
    }

    @Test
    @DisplayName("Private constructor can be invoked via reflection for class structure completeness")
    void privateConstructorCoverage() throws Exception {
        Constructor<TiepDonReadKiemToan> constructor = TiepDonReadKiemToan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TiepDonReadKiemToan instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("sameActor tests across valid, null, invalid, and mismatched contexts")
    class SameActorTests {

        @Test
        @DisplayName("Returns true when AuthorizationContext and AuditActorContext share identical valid identity and scope")
        void returnsTrueForMatchingValidActorAndContext() {
            assertTrue(TiepDonReadKiemToan.sameActor(validContext, validActor));
        }

        @Test
        @DisplayName("Returns false when context is null")
        void returnsFalseWhenContextIsNull() {
            assertFalse(TiepDonReadKiemToan.sameActor(null, validActor));
        }

        @Test
        @DisplayName("Returns false when actor is null")
        void returnsFalseWhenActorIsNull() {
            assertFalse(TiepDonReadKiemToan.sameActor(validContext, null));
        }

        @Test
        @DisplayName("Returns false when both context and actor are null")
        void returnsFalseWhenBothAreNull() {
            assertFalse(TiepDonReadKiemToan.sameActor(null, null));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive employeeId")
        void returnsFalseWhenContextHasInvalidEmployeeId() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    0, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive roleId")
        void returnsFalseWhenContextHasInvalidRoleId() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, -1, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to null roleCode")
        void returnsFalseWhenContextHasNullRoleCode() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, null, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to blank roleCode")
        void returnsFalseWhenContextHasBlankRoleCode() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, "   ", FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive facilityId")
        void returnsFalseWhenContextHasInvalidFacilityId() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, 0, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive departmentId")
        void returnsFalseWhenContextHasInvalidDepartmentId() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, -5, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to null sessionId")
        void returnsFalseWhenContextHasNullSessionId() {
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, null
            );
            assertFalse(TiepDonReadKiemToan.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when employeeId mismatches")
        void returnsFalseWhenEmployeeIdMismatches() {
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID + 100, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when roleId mismatches")
        void returnsFalseWhenRoleIdMismatches() {
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID + 10, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when roleCode mismatches")
        void returnsFalseWhenRoleCodeMismatches() {
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when facilityId mismatches")
        void returnsFalseWhenFacilityIdMismatches() {
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID + 1, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when departmentId mismatches")
        void returnsFalseWhenDepartmentIdMismatches() {
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID + 1, SESSION_ID
            );
            assertFalse(TiepDonReadKiemToan.sameActor(mismatched, validActor));
        }
    }

    @Nested
    @DisplayName("evaluate tests across null, mismatched, and valid authorization scenarios")
    class EvaluateTests {

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when authorization service is null")
        void deniesWhenAuthorizationServiceIsNull() {
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    validContext, validActor, null, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context is null")
        void deniesWhenContextIsNull() {
            PhanQuyenDichVu authService = createAuthorizationService(true, true);
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    null, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when actor is null")
        void deniesWhenActorIsNull() {
            PhanQuyenDichVu authService = createAuthorizationService(true, true);
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    validContext, null, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context is invalid")
        void deniesWhenContextIsInvalid() {
            PhanQuyenDichVu authService = createAuthorizationService(true, true);
            PhanQuyenNguCanh invalid = new PhanQuyenNguCanh(
                    -1, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    invalid, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context and actor fields mismatch")
        void deniesWhenContextAndActorMismatch() {
            PhanQuyenDichVu authService = createAuthorizationService(true, true);
            PhanQuyenNguCanh mismatched = new PhanQuyenNguCanh(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID + 99, DEPARTMENT_ID, SESSION_ID
            );
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    mismatched, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Delegates to AuthorizationService and returns allow decision when authorized")
        void returnsAllowWhenAuthorized() {
            PhanQuyenDichVu authService = createAuthorizationService(true, true);
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    validContext, validActor, authService, MODULE
            );
            assertTrue(decision.allowed());
            assertEquals(ReasonCode.ALLOWED, decision.reasonCode());
        }

        @Test
        @DisplayName("Returns denied decision when permission matrix denies the request")
        void returnsDeniedWhenPermissionDeniedInMatrix() {
            PhanQuyenDichVu authService = createAuthorizationService(false, true);
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    validContext, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.PERMISSION_DENIED, decision.reasonCode());
        }

        @Test
        @DisplayName("Returns denied decision when permission grant is missing")
        void returnsDeniedWhenPermissionMissing() {
            PhanQuyenDichVu authService = createAuthorizationService(true, false);
            PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(
                    validContext, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.PERMISSION_MISSING, decision.reasonCode());
        }
    }

    @Nested
    @DisplayName("success audit logging tests")
    class SuccessTests {

        @Test
        @DisplayName("Appends independent SystemAuditEntry with SUCCESS outcome and null reason code")
        void successAppendsIndependentAuditEntry() {
            TiepDonReadKiemToan.success(auditPort, validActor, KiemToanHanhDongMa.PATIENT_SEARCH);

            assertEquals(1, auditPort.independentEntries.size());
            assertTrue(auditPort.currentTxEntries.isEmpty());

            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(EMPLOYEE_ID, entry.actorEmployeeId());
            assertEquals(ROLE_ID, entry.roleId());
            assertEquals(ROLE_CODE, entry.roleCode());
            assertEquals(FACILITY_ID, entry.facilityId());
            assertEquals(DEPARTMENT_ID, entry.departmentId());
            assertEquals(KiemToanHanhDongMa.PATIENT_SEARCH, entry.actionCode());
            assertNull(entry.resourceType());
            assertNull(entry.resourceId());
            assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
            assertNull(entry.reasonCode());
            assertNotNull(entry.occurredAtUtc());
            assertEquals(WORKSTATION_ID, entry.workstationId());
            assertEquals(CORRELATION_ID, entry.correlationId());
        }

        @Test
        @DisplayName("Supports various read audit action codes such as PATIENT_VIEW")
        void successSupportsPatientView() {
            TiepDonReadKiemToan.success(auditPort, validActor, KiemToanHanhDongMa.PATIENT_VIEW);

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.PATIENT_VIEW, entry.actionCode());
            assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
            assertNull(entry.reasonCode());
        }
    }

    @Nested
    @DisplayName("denied audit logging tests across decision reasons and safe failure modes")
    class DeniedTests {

        @Test
        @DisplayName("Safely ignores denial when audit port is null")
        void safelyIgnoresWhenAuditPortIsNull() {
            assertDoesNotThrow(() -> TiepDonReadKiemToan.denied(
                    null, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED)
            ));
        }

        @Test
        @DisplayName("Safely ignores denial when actor is null")
        void safelyIgnoresWhenActorIsNull() {
            assertDoesNotThrow(() -> TiepDonReadKiemToan.denied(
                    auditPort, null, PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED)
            ));
            assertTrue(auditPort.independentEntries.isEmpty());
        }

        @Test
        @DisplayName("Maps SCOPE_INVALID decision reason to AuditReasonCode.SCOPE_MISMATCH")
        void mapsScopeInvalidToScopeMismatch() {
            TiepDonReadKiemToan.denied(
                    auditPort, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.SCOPE_INVALID)
            );

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, entry.outcome());
            assertEquals(KiemToanLyDoMa.SCOPE_MISMATCH, entry.reasonCode());
            assertEquals(EMPLOYEE_ID, entry.actorEmployeeId());
            assertEquals(ROLE_ID, entry.roleId());
            assertEquals(ROLE_CODE, entry.roleCode());
            assertEquals(FACILITY_ID, entry.facilityId());
            assertEquals(DEPARTMENT_ID, entry.departmentId());
            assertEquals(WORKSTATION_ID, entry.workstationId());
            assertEquals(CORRELATION_ID, entry.correlationId());
            assertNull(entry.resourceType());
            assertNull(entry.resourceId());
            assertNotNull(entry.occurredAtUtc());
        }

        @Test
        @DisplayName("Maps TARGET_MISMATCH decision reason to AuditReasonCode.SCOPE_MISMATCH")
        void mapsTargetMismatchToScopeMismatch() {
            TiepDonReadKiemToan.denied(
                    auditPort, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.TARGET_MISMATCH)
            );

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, entry.outcome());
            assertEquals(KiemToanLyDoMa.SCOPE_MISMATCH, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps INVALID_CONTEXT decision reason to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsInvalidContextToAuthForbidden() {
            TiepDonReadKiemToan.denied(
                    auditPort, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.INVALID_CONTEXT)
            );

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, entry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps PERMISSION_DENIED decision reason to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsPermissionDeniedToAuthForbidden() {
            TiepDonReadKiemToan.denied(
                    auditPort, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED)
            );

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, entry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps null decision to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsNullDecisionToAuthForbidden() {
            TiepDonReadKiemToan.denied(auditPort, validActor, null);

            assertEquals(1, auditPort.independentEntries.size());
            HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, entry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Catches and suppresses RuntimeException thrown by independent audit store")
        void suppressesAuditStoreRuntimeException() {
            auditPort.throwOnAppendIndependently = true;

            assertDoesNotThrow(() -> TiepDonReadKiemToan.denied(
                    auditPort, validActor, PhanQuyenQuyetDinh.deny(ReasonCode.PERMISSION_DENIED)
            ));
        }
    }

    private PhanQuyenDichVu createAuthorizationService(boolean grantAllowed, boolean grantPresent) {
        HoatDongPhienCongKetNoi sessionPort = sessionId -> Optional.of(
                new HoatDongPhienCongKetNoi.ActiveSession(
                        sessionId, EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, true, true, true
                )
        );

        INhanVienRepository employeeRepo = (INhanVienRepository) Proxy.newProxyInstance(
                INhanVienRepository.class.getClassLoader(),
                new Class<?>[]{INhanVienRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> {
                        NhanVien employee = new NhanVien();
                        employee.setId(EMPLOYEE_ID);
                        employee.setHoatDong(true);
                        employee.setDeleted(false);
                        yield Optional.of(employee);
                    }
                    case "getAssignedRoles" -> {
                        VaiTro role = new VaiTro();
                        role.setId(ROLE_ID);
                        role.setTenVaiTro(ROLE_CODE);
                        role.setDeleted(false);
                        yield List.of(role);
                    }
                    case "getAssignedWorkScopes" -> List.of(
                            new NhanVienWorkPhamVi(FACILITY_ID, "Cơ sở 1", DEPARTMENT_ID, "Khoa Tiếp đón")
                    );
                    case "toString" -> "TestEmployeeRepository";
                    default -> null;
                }
        );

        IHeThongRepository systemRepo = new IHeThongRepository() {
            @Override
            public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                if (!grantPresent) {
                    return List.of();
                }
                MaTranPhanQuyen grant = new MaTranPhanQuyen();
                grant.setMaVaiTro(roleId);
                grant.setPhanHe(MODULE);
                grant.setHanhDong(HanhDongQuyen.XEM);
                grant.setDuocPhep(grantAllowed);
                grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
                grant.setDeleted(false);
                return List.of(grant);
            }

            @Override
            public Optional<String> getSystemParameterValue(String key) {
                return Optional.empty();
            }
        };

        return new PhanQuyenDichVu(sessionPort, employeeRepo, systemRepo);
    }

    private static class TestAuditWritePort implements KiemToanWriteCongKetNoi {
        private final List<HeThongKiemToanNhapLieu> currentTxEntries = new ArrayList<>();
        private final List<HeThongKiemToanNhapLieu> independentEntries = new ArrayList<>();
        private final List<YTeRecordKiemToanNhapLieu> medicalRecordTxEntries = new ArrayList<>();
        private final List<YTeRecordKiemToanNhapLieu> medicalRecordIndependentEntries = new ArrayList<>();
        private boolean throwOnAppendIndependently = false;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            currentTxEntries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            if (throwOnAppendIndependently) {
                throw new RuntimeException("Simulated independent audit write failure");
            }
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            medicalRecordTxEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            medicalRecordIndependentEntries.add(entry);
        }
    }
}

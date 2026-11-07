package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationDecision.ReasonCode;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionReadAuditTest {

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
    private AuditActorContext validActor;
    private AuthorizationContext validContext;

    @BeforeEach
    void setUp() {
        auditPort = new TestAuditWritePort();
        validActor = new AuditActorContext(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID
        );
        validContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
        );
    }

    @Test
    @DisplayName("Private constructor can be invoked via reflection for class structure completeness")
    void privateConstructorCoverage() throws Exception {
        Constructor<ReceptionReadAudit> constructor = ReceptionReadAudit.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ReceptionReadAudit instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("sameActor tests across valid, null, invalid, and mismatched contexts")
    class SameActorTests {

        @Test
        @DisplayName("Returns true when AuthorizationContext and AuditActorContext share identical valid identity and scope")
        void returnsTrueForMatchingValidActorAndContext() {
            assertTrue(ReceptionReadAudit.sameActor(validContext, validActor));
        }

        @Test
        @DisplayName("Returns false when context is null")
        void returnsFalseWhenContextIsNull() {
            assertFalse(ReceptionReadAudit.sameActor(null, validActor));
        }

        @Test
        @DisplayName("Returns false when actor is null")
        void returnsFalseWhenActorIsNull() {
            assertFalse(ReceptionReadAudit.sameActor(validContext, null));
        }

        @Test
        @DisplayName("Returns false when both context and actor are null")
        void returnsFalseWhenBothAreNull() {
            assertFalse(ReceptionReadAudit.sameActor(null, null));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive employeeId")
        void returnsFalseWhenContextHasInvalidEmployeeId() {
            AuthorizationContext invalid = new AuthorizationContext(
                    0, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive roleId")
        void returnsFalseWhenContextHasInvalidRoleId() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, -1, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to null roleCode")
        void returnsFalseWhenContextHasNullRoleCode() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, null, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to blank roleCode")
        void returnsFalseWhenContextHasBlankRoleCode() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, "   ", FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive facilityId")
        void returnsFalseWhenContextHasInvalidFacilityId() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, 0, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to non-positive departmentId")
        void returnsFalseWhenContextHasInvalidDepartmentId() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, -5, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when context is invalid due to null sessionId")
        void returnsFalseWhenContextHasNullSessionId() {
            AuthorizationContext invalid = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, null
            );
            assertFalse(ReceptionReadAudit.sameActor(invalid, validActor));
        }

        @Test
        @DisplayName("Returns false when employeeId mismatches")
        void returnsFalseWhenEmployeeIdMismatches() {
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID + 100, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when roleId mismatches")
        void returnsFalseWhenRoleIdMismatches() {
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID + 10, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when roleCode mismatches")
        void returnsFalseWhenRoleCodeMismatches() {
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when facilityId mismatches")
        void returnsFalseWhenFacilityIdMismatches() {
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID + 1, DEPARTMENT_ID, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(mismatched, validActor));
        }

        @Test
        @DisplayName("Returns false when departmentId mismatches")
        void returnsFalseWhenDepartmentIdMismatches() {
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID + 1, SESSION_ID
            );
            assertFalse(ReceptionReadAudit.sameActor(mismatched, validActor));
        }
    }

    @Nested
    @DisplayName("evaluate tests across null, mismatched, and valid authorization scenarios")
    class EvaluateTests {

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when authorization service is null")
        void deniesWhenAuthorizationServiceIsNull() {
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    validContext, validActor, null, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context is null")
        void deniesWhenContextIsNull() {
            AuthorizationService authService = createAuthorizationService(true, true);
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    null, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when actor is null")
        void deniesWhenActorIsNull() {
            AuthorizationService authService = createAuthorizationService(true, true);
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    validContext, null, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context is invalid")
        void deniesWhenContextIsInvalid() {
            AuthorizationService authService = createAuthorizationService(true, true);
            AuthorizationContext invalid = new AuthorizationContext(
                    -1, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID
            );
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    invalid, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Denies with INVALID_CONTEXT when context and actor fields mismatch")
        void deniesWhenContextAndActorMismatch() {
            AuthorizationService authService = createAuthorizationService(true, true);
            AuthorizationContext mismatched = new AuthorizationContext(
                    EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID + 99, DEPARTMENT_ID, SESSION_ID
            );
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    mismatched, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.INVALID_CONTEXT, decision.reasonCode());
        }

        @Test
        @DisplayName("Delegates to AuthorizationService and returns allow decision when authorized")
        void returnsAllowWhenAuthorized() {
            AuthorizationService authService = createAuthorizationService(true, true);
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    validContext, validActor, authService, MODULE
            );
            assertTrue(decision.allowed());
            assertEquals(ReasonCode.ALLOWED, decision.reasonCode());
        }

        @Test
        @DisplayName("Returns denied decision when permission matrix denies the request")
        void returnsDeniedWhenPermissionDeniedInMatrix() {
            AuthorizationService authService = createAuthorizationService(false, true);
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
                    validContext, validActor, authService, MODULE
            );
            assertFalse(decision.allowed());
            assertEquals(ReasonCode.PERMISSION_DENIED, decision.reasonCode());
        }

        @Test
        @DisplayName("Returns denied decision when permission grant is missing")
        void returnsDeniedWhenPermissionMissing() {
            AuthorizationService authService = createAuthorizationService(true, false);
            AuthorizationDecision decision = ReceptionReadAudit.evaluate(
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
            ReceptionReadAudit.success(auditPort, validActor, AuditActionCode.PATIENT_SEARCH);

            assertEquals(1, auditPort.independentEntries.size());
            assertTrue(auditPort.currentTxEntries.isEmpty());

            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(EMPLOYEE_ID, entry.actorEmployeeId());
            assertEquals(ROLE_ID, entry.roleId());
            assertEquals(ROLE_CODE, entry.roleCode());
            assertEquals(FACILITY_ID, entry.facilityId());
            assertEquals(DEPARTMENT_ID, entry.departmentId());
            assertEquals(AuditActionCode.PATIENT_SEARCH, entry.actionCode());
            assertNull(entry.resourceType());
            assertNull(entry.resourceId());
            assertEquals(AuditOutcome.SUCCESS, entry.outcome());
            assertNull(entry.reasonCode());
            assertNotNull(entry.occurredAtUtc());
            assertEquals(WORKSTATION_ID, entry.workstationId());
            assertEquals(CORRELATION_ID, entry.correlationId());
        }

        @Test
        @DisplayName("Supports various read audit action codes such as PATIENT_VIEW")
        void successSupportsPatientView() {
            ReceptionReadAudit.success(auditPort, validActor, AuditActionCode.PATIENT_VIEW);

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.PATIENT_VIEW, entry.actionCode());
            assertEquals(AuditOutcome.SUCCESS, entry.outcome());
            assertNull(entry.reasonCode());
        }
    }

    @Nested
    @DisplayName("denied audit logging tests across decision reasons and safe failure modes")
    class DeniedTests {

        @Test
        @DisplayName("Safely ignores denial when audit port is null")
        void safelyIgnoresWhenAuditPortIsNull() {
            assertDoesNotThrow(() -> ReceptionReadAudit.denied(
                    null, validActor, AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED)
            ));
        }

        @Test
        @DisplayName("Safely ignores denial when actor is null")
        void safelyIgnoresWhenActorIsNull() {
            assertDoesNotThrow(() -> ReceptionReadAudit.denied(
                    auditPort, null, AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED)
            ));
            assertTrue(auditPort.independentEntries.isEmpty());
        }

        @Test
        @DisplayName("Maps SCOPE_INVALID decision reason to AuditReasonCode.SCOPE_MISMATCH")
        void mapsScopeInvalidToScopeMismatch() {
            ReceptionReadAudit.denied(
                    auditPort, validActor, AuthorizationDecision.deny(ReasonCode.SCOPE_INVALID)
            );

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.SCOPE_MISMATCH, entry.reasonCode());
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
            ReceptionReadAudit.denied(
                    auditPort, validActor, AuthorizationDecision.deny(ReasonCode.TARGET_MISMATCH)
            );

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.SCOPE_MISMATCH, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps INVALID_CONTEXT decision reason to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsInvalidContextToAuthForbidden() {
            ReceptionReadAudit.denied(
                    auditPort, validActor, AuthorizationDecision.deny(ReasonCode.INVALID_CONTEXT)
            );

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps PERMISSION_DENIED decision reason to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsPermissionDeniedToAuthForbidden() {
            ReceptionReadAudit.denied(
                    auditPort, validActor, AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED)
            );

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Maps null decision to AuditReasonCode.AUTH_FORBIDDEN")
        void mapsNullDecisionToAuthForbidden() {
            ReceptionReadAudit.denied(auditPort, validActor, null);

            assertEquals(1, auditPort.independentEntries.size());
            SystemAuditEntry entry = auditPort.independentEntries.get(0);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
        }

        @Test
        @DisplayName("Catches and suppresses RuntimeException thrown by independent audit store")
        void suppressesAuditStoreRuntimeException() {
            auditPort.throwOnAppendIndependently = true;

            assertDoesNotThrow(() -> ReceptionReadAudit.denied(
                    auditPort, validActor, AuthorizationDecision.deny(ReasonCode.PERMISSION_DENIED)
            ));
        }
    }

    private AuthorizationService createAuthorizationService(boolean grantAllowed, boolean grantPresent) {
        ActiveSessionPort sessionPort = sessionId -> Optional.of(
                new ActiveSessionPort.ActiveSession(
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
                            new NhanVienWorkScope(FACILITY_ID, "Cơ sở 1", DEPARTMENT_ID, "Khoa Tiếp đón")
                    );
                    case "toString" -> "TestEmployeeRepository";
                    default -> null;
                }
        );

        ISystemRepository systemRepo = new ISystemRepository() {
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

        return new AuthorizationService(sessionPort, employeeRepo, systemRepo);
    }

    private static class TestAuditWritePort implements AuditWritePort {
        private final List<SystemAuditEntry> currentTxEntries = new ArrayList<>();
        private final List<SystemAuditEntry> independentEntries = new ArrayList<>();
        private final List<MedicalRecordAuditEntry> medicalRecordTxEntries = new ArrayList<>();
        private final List<MedicalRecordAuditEntry> medicalRecordIndependentEntries = new ArrayList<>();
        private boolean throwOnAppendIndependently = false;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            currentTxEntries.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            if (throwOnAppendIndependently) {
                throw new RuntimeException("Simulated independent audit write failure");
            }
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            medicalRecordTxEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            medicalRecordIndependentEntries.add(entry);
        }
    }
}

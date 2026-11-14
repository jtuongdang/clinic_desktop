package vn.clinicmanagement.application.patienthistory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetBenhNhanTienSuHandlerTest {
    private static final int PATIENT_ID = 52;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");

    @Test
    void doctorWithExactScopeGrantQueriesThenAuditsBeforeReturningHistory() {
        Fixture fixture = new Fixture();
        TienSuBenh history = history(91);
        fixture.histories = List.of(history);

        KetQua<List<TienSuBenh>> result = fixture.handle(fixture.context(), fixture.actor());

        assertTrue(result.isSuccess());
        assertEquals(List.of("query", "audit"), fixture.order);
        assertEquals(PATIENT_ID, fixture.patientId);
        assertEquals(FACILITY_ID, fixture.facilityId);
        assertEquals(DEPARTMENT_ID, fixture.departmentId);
        assertEquals(List.of(history), result.valueOrThrow());
        HeThongKiemToanNhapLieu entry = fixture.audit.entries.get(0);
        assertEquals(KiemToanHanhDongMa.PATIENT_VIEW, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.PATIENT, entry.resourceType());
        assertEquals(PATIENT_ID, entry.resourceId());
        assertEquals(EMPLOYEE_ID, entry.actorEmployeeId());
        assertEquals(ROLE_ID, entry.roleId());
        assertEquals("DOCTOR", entry.roleCode());
        assertEquals(FACILITY_ID, entry.facilityId());
        assertEquals(DEPARTMENT_ID, entry.departmentId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertNotNull(entry.occurredAtUtc());
        assertEquals(fixture.correlationId, entry.correlationId());
    }

    @Test
    void validDoctorReadWithNoRowsStillWritesAudit() {
        Fixture fixture = new Fixture();

        KetQua<List<TienSuBenh>> result = fixture.handle(fixture.context(), fixture.actor());

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().isEmpty());
        assertEquals(List.of("query", "audit"), fixture.order);
    }

    @Test
    void nonDoctorWithPatientViewGrantIsDeniedWithoutQuery() {
        Fixture fixture = new Fixture();
        fixture.roleCode = "RECEPTIONIST";

        KetQua<List<TienSuBenh>> result = fixture.handle(fixture.context(), fixture.actor());

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.repositoryCalls);
        assertEquals(0, fixture.sessionChecks);
        assertTrue(fixture.audit.entries.isEmpty());
    }

    @Test
    void mismatchedActorOrRevalidatedSessionIsDeniedWithoutQuery() {
        Fixture fixture = new Fixture();
        KiemToanTacNhanNguCanh mismatchedActor = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID + 1, DEPARTMENT_ID,
                "synthetic-workstation", fixture.correlationId);
        assertTrue(fixture.handle(fixture.context(), mismatchedActor).isFailure());
        assertEquals(0, fixture.repositoryCalls);
        assertEquals(0, fixture.sessionChecks);

        fixture = new Fixture();
        fixture.activeSessionId = UUID.randomUUID();
        assertTrue(fixture.handle(fixture.context(), fixture.actor()).isFailure());
        assertEquals(0, fixture.repositoryCalls);
        assertEquals(1, fixture.sessionChecks);
        assertTrue(fixture.audit.entries.isEmpty());
    }

    @Test
    void missingPatientGrantOrExactAssignedScopeIsDeniedWithoutQuery() {
        Fixture fixture = new Fixture();
        fixture.patientGrant = false;
        assertTrue(fixture.handle(fixture.context(), fixture.actor()).isFailure());
        assertEquals(0, fixture.repositoryCalls);

        fixture = new Fixture();
        fixture.exactScopeAssigned = false;
        assertTrue(fixture.handle(fixture.context(), fixture.actor()).isFailure());
        assertEquals(0, fixture.repositoryCalls);
        assertTrue(fixture.audit.entries.isEmpty());
    }

    @Test
    void invalidPatientOrMissingContextDoesNotQuery() {
        Fixture fixture = new Fixture();
        KetQua<List<TienSuBenh>> invalidPatient = fixture.handle(
                new GetBenhNhanTienSuQuery(0, fixture.context(), fixture.actor()));
        KetQua<List<TienSuBenh>> missingContext = fixture.handle(
                new GetBenhNhanTienSuQuery(PATIENT_ID, null, fixture.actor()));

        assertTrue(invalidPatient.isFailure());
        assertTrue(missingContext.isFailure());
        assertEquals(0, fixture.repositoryCalls);
        assertEquals(0, fixture.sessionChecks);
    }

    @Test
    void auditFailureReturnsNoHistory() {
        Fixture fixture = new Fixture();
        fixture.histories = List.of(history(92));
        fixture.audit.failure = new IllegalStateException("synthetic audit unavailable");

        KetQua<List<TienSuBenh>> result = fixture.handle(fixture.context(), fixture.actor());

        assertTrue(result.isFailure());
        assertEquals("PATIENT_HISTORY_AUDIT_FAILED", result.getError().orElseThrow().code());
        assertTrue(result.getValue().isEmpty());
        assertEquals(List.of("query", "audit"), fixture.order);
    }

    @Test
    void repositoryFailureDoesNotWriteSuccessfulReadAudit() {
        Fixture fixture = new Fixture();
        fixture.repositoryFailure = new IllegalStateException("synthetic query unavailable");

        KetQua<List<TienSuBenh>> result = fixture.handle(fixture.context(), fixture.actor());

        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertEquals(List.of("query"), fixture.order);
        assertTrue(fixture.audit.entries.isEmpty());
    }

    private static TienSuBenh history(int id) {
        TienSuBenh history = new TienSuBenh();
        history.setId(id);
        history.setMaBenhNhan(PATIENT_ID);
        history.setLoaiTienSu(LoaiTienSu.BENH_NEN);
        history.setMoTa("synthetic history narrative");
        history.setOwnershipScope(FACILITY_ID, DEPARTMENT_ID);
        return history;
    }

    private static MaTranPhanQuyen patientViewGrant() {
        MaTranPhanQuyen grant = new MaTranPhanQuyen();
        grant.setMaVaiTro(ROLE_ID);
        grant.setPhanHe("PATIENT");
        grant.setHanhDong(HanhDongQuyen.XEM);
        grant.setDuocPhep(true);
        grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        return grant;
    }

    private static final class Fixture {
        final UUID correlationId = UUID.fromString("8be904d2-8d55-4c7f-9f4b-54d8e8435bbd");
        final List<String> order = new ArrayList<>();
        final RecordingAudit audit = new RecordingAudit(order);
        String roleCode = "DOCTOR";
        UUID activeSessionId = SESSION_ID;
        boolean active = true;
        boolean exactScopeAssigned = true;
        boolean patientGrant = true;
        int sessionChecks;
        int repositoryCalls;
        int patientId;
        int facilityId;
        int departmentId;
        RuntimeException repositoryFailure;
        List<TienSuBenh> histories = List.of();
        final ITienSuBenhRepository repository = new ITienSuBenhRepository() {
            @Override public List<TienSuBenh> getHistoryByPatientAndScope(
                    int patientId, int facilityId, int departmentId) {
                order.add("query");
                repositoryCalls++;
                Fixture.this.patientId = patientId;
                Fixture.this.facilityId = facilityId;
                Fixture.this.departmentId = departmentId;
                if (repositoryFailure != null) throw repositoryFailure;
                return histories;
            }
            @Override public Optional<TienSuBenh> getActiveByIdPatientAndScope(
                    int historyId, int patientId, int facilityId, int departmentId) {
                throw new UnsupportedOperationException("Read test does not update history.");
            }
            @Override public TienSuBenh add(TienSuBenh history) {
                throw new UnsupportedOperationException("Read test does not add history.");
            }
            @Override public boolean update(TienSuBenh history, long expectedVersion) {
                throw new UnsupportedOperationException("Read test does not update history.");
            }
        };
        final PhanQuyenDichVu authorization;

        Fixture() {
            NhanVien employee = new NhanVien();
            employee.setId(EMPLOYEE_ID);
            INhanVienRepository employees = (INhanVienRepository) java.lang.reflect.Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> Optional.of(employee);
                        case "getAssignedRoles" -> {
                            VaiTro assignedRole = new VaiTro();
                            assignedRole.setId(ROLE_ID);
                            assignedRole.setTenVaiTro(roleCode);
                            yield List.of(assignedRole);
                        }
                        case "getAssignedWorkScopes" -> List.of(new NhanVienWorkPhamVi(
                                FACILITY_ID, "Synthetic facility",
                                exactScopeAssigned ? DEPARTMENT_ID : DEPARTMENT_ID + 1, "Synthetic department"));
                        default -> defaultValue(method.getReturnType());
                    });
            IHeThongRepository system = new IHeThongRepository() {
                @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                    return patientGrant ? List.of(patientViewGrant()) : List.of();
                }
                @Override public Optional<String> getSystemParameterValue(String key) {
                    return Optional.empty();
                }
            };
            authorization = new PhanQuyenDichVu(sessionId -> {
                sessionChecks++;
                return Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(
                        activeSessionId, EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID,
                        active, true, true));
            }, employees, system);
        }

        PhanQuyenNguCanh context() {
            return new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        }

        KiemToanTacNhanNguCanh actor() {
            return new KiemToanTacNhanNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID,
                    "synthetic-workstation", correlationId);
        }

        KetQua<List<TienSuBenh>> handle(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
            return new GetBenhNhanTienSuHandler(repository, authorization, audit)
                    .handle(new GetBenhNhanTienSuQuery(PATIENT_ID, context, actor));
        }

        KetQua<List<TienSuBenh>> handle(GetBenhNhanTienSuQuery query) {
            return new GetBenhNhanTienSuHandler(repository, authorization, audit).handle(query);
        }
    }

    private static final class RecordingAudit implements KiemToanWriteCongKetNoi {
        final List<String> order;
        final List<HeThongKiemToanNhapLieu> entries = new ArrayList<>();
        RuntimeException failure;

        RecordingAudit(List<String> order) { this.order = order; }

        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            order.add("audit");
            if (failure != null) throw failure;
            entries.add(entry);
        }
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}

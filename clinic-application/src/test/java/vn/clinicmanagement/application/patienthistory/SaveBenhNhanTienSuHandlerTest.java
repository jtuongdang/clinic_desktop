package vn.clinicmanagement.application.patienthistory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.*;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.patient.*;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.util.*;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class SaveBenhNhanTienSuHandlerTest {
    private static final int PATIENT = 52, HISTORY = 91, EMPLOYEE = 17, ROLE = 4, FACILITY = 3, DEPARTMENT = 8;
    private static final UUID SESSION = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void createsOnlyApprovedCategoriesUsingTrustedScopeAndAuditsWithinTransaction() {
        for (LoaiTienSu category : List.of(LoaiTienSu.BENH_NEN, LoaiTienSu.DI_UNG, LoaiTienSu.PHAU_THUAT)) {
            Fixture f = new Fixture();
            KetQua<BenhNhanTienSuBienNhan> result = f.handler().handle(f.command(null, null, category, "synthetic narrative"));
            assertTrue(result.isSuccess());
            assertEquals(FACILITY, f.inserted.getMaCoSo());
            assertEquals(DEPARTMENT, f.inserted.getMaKhoa());
            assertEquals(EMPLOYEE, f.inserted.getCreatedBy());
            assertArrayEquals(new int[]{PATIENT, FACILITY, DEPARTMENT}, f.patientScope);
            assertTrue(f.audit.inTransaction);
            assertEquals(KiemToanHanhDongMa.PATIENT_UPDATED, f.audit.entry.actionCode());
            assertEquals(KiemToanTaiNguyenLoai.PATIENT, f.audit.entry.resourceType());
            assertEquals(PATIENT, f.audit.entry.resourceId());
            assertEquals(CORRELATION, f.audit.entry.correlationId());
            assertTrue(f.transactions.auditWasInTransaction);
            assertFalse(f.audit.entry.toString().contains("synthetic narrative"));
        }
    }

    @Test
    void updatesOnlyExactActiveRowAndPreservesCreationAndOwnershipMetadata() {
        Fixture f = new Fixture();
        TienSuBenh row = history(FACILITY, DEPARTMENT, PATIENT, false, 2L);
        row.setCreatedBy(6);
        row.setId(HISTORY);
        f.existing = row;
        KetQua<BenhNhanTienSuBienNhan> result = f.handler().handle(f.command(HISTORY, 2L, LoaiTienSu.DI_UNG, "updated synthetic"));
        assertTrue(result.isSuccess());
        assertEquals(6, f.updated.getCreatedBy());
        assertEquals(EMPLOYEE, f.updated.getUpdatedBy());
        assertEquals(FACILITY, f.updated.getMaCoSo());
        assertEquals(DEPARTMENT, f.updated.getMaKhoa());
        assertEquals(2L, f.updateExpectedVersion);
    }

    @Test
    void rejectsInvalidPayloadBeforeAnyAuthorizationOrPatientAccess() {
        for (SaveBenhNhanTienSuCommand command : List.of(
                new SaveBenhNhanTienSuCommand(0, null, null, LoaiTienSu.DI_UNG, "x", context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, -1, 1L, LoaiTienSu.DI_UNG, "x", context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, null, null, LoaiTienSu.TIEN_SU_GIA_DINH, "x", context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, null, null, null, "x", context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, null, null, LoaiTienSu.DI_UNG, " ", context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, null, null, LoaiTienSu.DI_UNG, null, context(), actor()),
                new SaveBenhNhanTienSuCommand(PATIENT, null, null, LoaiTienSu.DI_UNG, "x".repeat(1001), context(), actor()))) {
            Fixture f = new Fixture();
            assertTrue(f.handler().handle(command).isFailure());
            assertEquals(0, f.auth.lookups);
            assertEquals(0, f.patientReads);
            assertNull(f.inserted);
        }
    }

    @Test
    void rejectsMissingOrInvalidContextsBeforePatientAccess() {
        Fixture f = new Fixture();
        SaveBenhNhanTienSuCommand missingContexts = new SaveBenhNhanTienSuCommand(
                PATIENT, null, null, LoaiTienSu.DI_UNG, "synthetic", null, null);
        assertTrue(f.handler().handle(missingContexts).isFailure());
        assertEquals(0, f.auth.lookups);
        assertEquals(0, f.patientReads);
        assertNull(f.inserted);

        f = new Fixture();
        SaveBenhNhanTienSuCommand invalidSession = new SaveBenhNhanTienSuCommand(
                PATIENT, null, null, LoaiTienSu.DI_UNG, "synthetic",
                new PhanQuyenNguCanh(EMPLOYEE, ROLE, "DOCTOR", FACILITY, DEPARTMENT, null), actor());
        assertTrue(f.handler().handle(invalidSession).isFailure());
        assertEquals(0, f.auth.lookups);
        assertEquals(0, f.patientReads);
        assertNull(f.inserted);
    }

    @Test
    void deniesNonDoctorMissingGrantMismatchedActorAndWrongPatientScopeBeforeMutation() {
        Fixture f = new Fixture();
        f.roleCode = "RECEPTIONIST";
        assertDeniedWithoutMutation(f);
        f = new Fixture();
        f.grant = false;
        assertDeniedWithoutMutation(f);
        f = new Fixture();
        f.auth.active = false;
        assertDeniedWithoutMutation(f);
        f = new Fixture();
        f.auth.sessionDepartment = DEPARTMENT + 1;
        assertDeniedWithoutMutation(f);
        f = new Fixture();
        assertTrue(f.handler().handle(new SaveBenhNhanTienSuCommand(PATIENT, null, null, LoaiTienSu.DI_UNG,
                "synthetic", context(), new KiemToanTacNhanNguCanh(EMPLOYEE, ROLE, "DOCTOR", FACILITY + 1,
                DEPARTMENT, null, CORRELATION))).isFailure());
        assertEquals(0, f.auth.lookups);
        f = new Fixture();
        f.patientInScope = false;
        assertDeniedWithoutMutation(f);
    }

    @Test
    void deniesMissingLegacyDeletedWrongPatientAndWrongScopeRowsOnUpdate() {
        for (TienSuBenh existing : Arrays.asList(null, history(null, null, PATIENT, false, 2L),
                history(FACILITY, DEPARTMENT, PATIENT, true, 2L), history(FACILITY, DEPARTMENT, PATIENT + 1, false, 2L),
                history(FACILITY + 1, DEPARTMENT, PATIENT, false, 2L))) {
            Fixture f = new Fixture();
            if (existing != null) existing.setId(HISTORY);
            f.existing = existing;
            assertDeniedWithoutMutation(f, f.command(HISTORY, 2L, LoaiTienSu.BENH_NEN, "synthetic"));
        }
    }

    @Test
    void auditFailureRollsBackAndReturnsNoSuccessReceipt() {
        Fixture f = new Fixture();
        f.audit.failure = true;
        KetQua<BenhNhanTienSuBienNhan> result = f.handler().handle(f.command(null, null, LoaiTienSu.DI_UNG, "synthetic"));
        assertTrue(result.isFailure());
        assertTrue(f.transactions.mutationWasStaged);
        assertTrue(f.transactions.rolledBack);
        assertFalse(f.transactions.mutationCommitted);
        assertNull(f.transactions.committedValue);
    }

    private static void assertDeniedWithoutMutation(Fixture f) {
        assertDeniedWithoutMutation(f, f.command(null, null, LoaiTienSu.DI_UNG, "synthetic"));
    }

    private static void assertDeniedWithoutMutation(Fixture f, SaveBenhNhanTienSuCommand command) {
        assertTrue(f.handler().handle(command).isFailure());
        assertNull(f.inserted);
        assertNull(f.updated);
    }

    private static TienSuBenh history(Integer facility, Integer department, int patient, boolean deleted, long version) {
        TienSuBenh row = new TienSuBenh();
        row.setMaBenhNhan(patient);
        row.setLoaiTienSu(LoaiTienSu.DI_UNG);
        row.setMoTa("synthetic history");
        row.setOwnershipScope(facility, department);
        row.setDeleted(deleted);
        row.setVersion(version);
        return row;
    }

    private static PhanQuyenNguCanh context() { return new PhanQuyenNguCanh(EMPLOYEE, ROLE, "DOCTOR", FACILITY, DEPARTMENT, SESSION); }
    private static KiemToanTacNhanNguCanh actor() { return new KiemToanTacNhanNguCanh(EMPLOYEE, ROLE, "DOCTOR", FACILITY, DEPARTMENT, null, CORRELATION); }

    private static final class Fixture {
        String roleCode = "DOCTOR";
        boolean grant = true, patientInScope = true;
        int authLookups, patientReads;
        int[] patientScope;
        TienSuBenh inserted, updated, existing;
        long updateExpectedVersion;
        final AuthFixture auth = new AuthFixture();
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture(transactions);
        final ITienSuBenhRepository histories = (ITienSuBenhRepository) Proxy.newProxyInstance(
                ITienSuBenhRepository.class.getClassLoader(), new Class<?>[]{ITienSuBenhRepository.class}, (p, m, a) -> switch (m.getName()) {
                    case "getActiveByIdPatientAndScope" -> Optional.ofNullable(existing);
                    case "add" -> { inserted = (TienSuBenh) a[0]; inserted.setId(101); transactions.mutationWasStaged = true; yield inserted; }
                    case "update" -> { updated = (TienSuBenh) a[0]; updateExpectedVersion = (long) a[1]; transactions.mutationWasStaged = true; yield true; }
                    default -> null;
                });
        final IBenhNhanRepository patients = (IBenhNhanRepository) Proxy.newProxyInstance(
                IBenhNhanRepository.class.getClassLoader(), new Class<?>[]{IBenhNhanRepository.class}, (p, m, a) -> {
                    if (m.getName().equals("getByIdInScope")) { patientReads++; patientScope = new int[]{(int) a[0], (int) a[1], (int) a[2]}; BenhNhan patient = new BenhNhan(); patient.setId(PATIENT); return patientInScope ? Optional.of(patient) : Optional.empty(); }
                    return null;
                });
        SaveBenhNhanTienSuHandler handler() { auth.roleCode = roleCode; auth.grant = grant; return new SaveBenhNhanTienSuHandler(histories, patients, auth.service(), transactions, audit); }
        SaveBenhNhanTienSuCommand command(Integer historyId, Long version, LoaiTienSu category, String narrative) {
            return new SaveBenhNhanTienSuCommand(PATIENT, historyId, version, category, narrative, context(), actor());
        }
    }

    private static final class TransactionFixture implements GiaoDichRanhGioi {
        boolean rolledBack, current, auditWasInTransaction, mutationWasStaged, mutationCommitted;
        Object committedValue;
        @Override public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> work) {
            current = true;
            KetQua<T> result;
            try { result = work.get(); } finally { current = false; }
            if (result.isSuccess()) {
                committedValue = result.getValue().orElse(null);
                mutationCommitted = mutationWasStaged;
            } else {
                rolledBack = true;
                mutationCommitted = false;
            }
            return result;
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        final TransactionFixture transactions;
        boolean failure, inTransaction;
        HeThongKiemToanNhapLieu entry;
        AuditFixture(TransactionFixture transactions) { this.transactions = transactions; }
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu event) { inTransaction = true; transactions.auditWasInTransaction = transactions.current; if (failure) throw new IllegalStateException(); entry = event; }
        @Override public void appendIndependently(HeThongKiemToanNhapLieu event) { throw new UnsupportedOperationException(); }
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu event) { throw new UnsupportedOperationException(); }
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu event) { throw new UnsupportedOperationException(); }
    }

    private static final class AuthFixture {
        int lookups;
        String roleCode = "DOCTOR";
        boolean grant = true, active = true;
        int sessionDepartment = DEPARTMENT;
        PhanQuyenDichVu service() {
            var session = new HoatDongPhienCongKetNoi.ActiveSession(SESSION, EMPLOYEE, ROLE, roleCode, FACILITY, sessionDepartment, active, true, true);
            NhanVien employee = new NhanVien(); employee.setId(EMPLOYEE);
            INhanVienRepository employees = (INhanVienRepository) Proxy.newProxyInstance(INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class}, (p,m,a) -> switch (m.getName()) {
                case "getById" -> { lookups++; yield Optional.of(employee); }
                case "getAssignedRoles" -> List.of(role(ROLE, roleCode));
                case "getAssignedWorkScopes" -> List.of(new NhanVienWorkPhamVi(FACILITY, "Synthetic facility", DEPARTMENT, "Synthetic department"));
                default -> null;
            });
            IHeThongRepository system = new IHeThongRepository() {
                @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int id) { if (!grant) return List.of(); MaTranPhanQuyen p = new MaTranPhanQuyen(); p.setMaVaiTro(ROLE); p.setPhanHe("PATIENT"); p.setHanhDong(HanhDongQuyen.XEM); p.setDuocPhep(true); p.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG); return List.of(p); }
                @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
            };
            return new PhanQuyenDichVu(id -> Optional.of(session), employees, system);
        }
        private static VaiTro role(int id, String code) { VaiTro role = new VaiTro(); role.setId(id); role.setTenVaiTro(code); return role; }
    }
}

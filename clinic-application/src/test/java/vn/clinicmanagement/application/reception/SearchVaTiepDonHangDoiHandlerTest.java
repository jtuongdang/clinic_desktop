package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SearchVaTiepDonHangDoiHandlerTest {
    private static final int EMPLOYEE = 17, ROLE = 4, FACILITY = 3, DEPARTMENT = 8;
    private static final String ROLE_CODE = "RECEPTIONIST";
    private static final UUID SESSION = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");

    @Test
    void deniedAndStaleScopeReadsAuditDenialWithoutRunningProtectedQueries() {
        Fixture f = new Fixture();
        f.permissions = List.of();
        assertTrue(f.search("synthetic-term").isFailure());
        assertEquals(0, f.patientQueries);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, f.audit.entries.get(0).actionCode());
        assertNull(f.audit.entries.get(0).resourceId());
        assertNull(f.audit.entries.get(0).resourceType());

        f = new Fixture();
        PhanQuyenNguCanh stale = new PhanQuyenNguCanh(EMPLOYEE, ROLE, ROLE_CODE,
                FACILITY, DEPARTMENT + 1, SESSION);
        assertTrue(f.search("private-search-term", stale).isFailure());
        assertEquals(0, f.patientQueries);
        assertEquals(1, f.audit.entries.size());
        assertNull(f.audit.entries.get(0).resourceId());
    }

    @Test
    void inactiveSessionAndRevokedAssignmentSkipRepositoryReads() {
        Fixture f = new Fixture();
        f.sessionActive = false;
        assertTrue(f.search("synthetic").isFailure());
        assertEquals(0, f.patientQueries);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, f.audit.entries.get(0).actionCode());

        f = new Fixture();
        f.assignmentValid = false;
        assertTrue(f.search("synthetic").isFailure());
        assertEquals(0, f.patientQueries);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, f.audit.entries.get(0).actionCode());
    }

    @Test
    void patientQueryPrecedesIndependentAuditAndOnlyThenReturnsResults() {
        Fixture f = new Fixture();
        f.patientResults = List.of(patient(21));
        KetQua<List<BenhNhanSearchMatHang>> result = f.search("  BN-SYN-21  ");

        assertTrue(result.isSuccess());
        assertEquals(List.of("query", "audit"), f.order);
        assertEquals(1, result.valueOrThrow().size());
        assertEquals(1, f.insuranceLookups);
        assertEquals(KiemToanHanhDongMa.PATIENT_SEARCH, f.audit.entries.get(0).actionCode());
        assertNull(f.audit.entries.get(0).resourceId());
    }

    @Test
    void searchCapsScopedResultsAtFiftyAndProjectsLatestInsurance() {
        Fixture f = new Fixture();
        List<BenhNhan> patients = new ArrayList<>();
        for (int id = 1; id <= 60; id++) patients.add(patient(id));
        f.patientResults = patients;
        BaoHiem card = new BaoHiem();
        card.setId(71);
        card.setMaBenhNhan(1);
        card.setSoBaoHiem("SYNTHETIC-CARD");
        f.cardsByPatient.put(1, card);

        KetQua<List<BenhNhanSearchMatHang>> result = f.search("synthetic");

        assertTrue(result.isSuccess());
        assertEquals(50, result.valueOrThrow().size());
        assertSame(card, result.valueOrThrow().get(0).latestInsurance());
        assertEquals(50, f.insuranceLookups);
        assertEquals(50, f.requestedLimit);
    }

    @Test
    void insuranceReadFailureReturnsNoPartialPatientResultsOrSuccessAudit() {
        Fixture f = new Fixture();
        f.patientResults = List.of(patient(23));
        f.insuranceFailure = new IllegalStateException("synthetic insurance read failure");

        KetQua<List<BenhNhanSearchMatHang>> result = f.search("synthetic");

        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertTrue(f.audit.entries.isEmpty());
    }

    @Test
    void auditFailureReturnsNoProtectedData() {
        Fixture f = new Fixture();
        f.patientResults = List.of(patient(22));
        f.audit.failure = new IllegalStateException("synthetic audit storage failure");

        KetQua<List<BenhNhanSearchMatHang>> result = f.search("synthetic");

        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertEquals(List.of("query", "audit"), f.order);
    }

    @Test
    void queueAndRoomAuditFailuresReturnNoProtectedData() {
        Fixture f = new Fixture();
        f.permissions = List.of(grant("RECEPTION"));
        f.audit.failure = new IllegalStateException("synthetic audit storage failure");
        KetQua<TiepDonHangDoiAnhChup> queue = f.queueHandler().handle(f.queueQuery());
        assertTrue(queue.isFailure());
        assertTrue(queue.getValue().isEmpty());

        f = new Fixture();
        f.permissions = List.of(grant("CATALOG"));
        f.audit.failure = new IllegalStateException("synthetic audit storage failure");
        KetQua<List<PhongKham>> rooms = f.roomHandler().handle(f.roomQuery());
        assertTrue(rooms.isFailure());
        assertTrue(rooms.getValue().isEmpty());
    }

    @Test
    void queryFailureDoesNotCreateSuccessfulReadAudit() {
        Fixture f = new Fixture();
        f.queryFailure = new IllegalStateException("synthetic query failure");

        assertTrue(f.search("synthetic").isFailure());
        assertTrue(f.audit.entries.isEmpty());
    }

    @Test
    void emptyResultForValidSearchIsAudited() {
        Fixture f = new Fixture();

        KetQua<List<BenhNhanSearchMatHang>> result = f.search("no-such-synthetic-patient");

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().isEmpty());
        assertEquals("no-such-synthetic-patient", f.lastKeyword);
        assertEquals(List.of("query", "audit"), f.order);
        assertEquals(KiemToanHanhDongMa.PATIENT_SEARCH, f.audit.entries.get(0).actionCode());
    }

    @Test
    void blankSearchInputDoesNotRunQueryOrCreateSuccessfulReadAudit() {
        Fixture f = new Fixture();

        KetQua<List<BenhNhanSearchMatHang>> result = f.search("  \t ");

        assertTrue(result.isFailure());
        assertEquals("SEARCH_KEYWORD_REQUIRED", result.getError().orElseThrow().code());
        assertEquals(0, f.patientQueries);
        assertTrue(f.audit.entries.isEmpty());
    }

    @Test
    void queueAndRoomReadsRequireTheirOwnGrantsAndAuditAfterQuery() {
        Fixture f = new Fixture();
        f.permissions = List.of(grant("RECEPTION"));
        KetQua<TiepDonHangDoiAnhChup> queueResult = f.queueHandler().handle(f.queueQuery());
        assertTrue(queueResult.isSuccess());
        assertEquals(List.of("queue", "summaries", "audit"), f.order);
        assertEquals(KiemToanHanhDongMa.PATIENT_VIEW, f.audit.entries.get(0).actionCode());
        assertEquals(FACILITY, f.queueFacility);
        assertEquals(DEPARTMENT, f.queueDepartment);
        assertEquals(FACILITY, f.summaryFacility);
        assertEquals(DEPARTMENT, f.summaryDepartment);

        f = new Fixture();
        f.permissions = List.of();
        assertTrue(f.queueHandler().handle(f.queueQuery()).isFailure());
        assertEquals(List.of("audit"), f.order);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, f.audit.entries.get(0).actionCode());

        f = new Fixture();
        f.permissions = List.of();
        assertTrue(f.roomHandler().handle(f.roomQuery()).isFailure());
        assertEquals(List.of("audit"), f.order);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, f.audit.entries.get(0).actionCode());

        f = new Fixture();
        f.permissions = List.of(grant("CATALOG"));
        KetQua<List<PhongKham>> roomResult = f.roomHandler().handle(f.roomQuery());
        assertTrue(roomResult.isSuccess());
        assertEquals(List.of("rooms", "audit"), f.order);
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, f.audit.entries.get(0).actionCode());
        assertEquals(FACILITY, f.roomFacility);
        assertEquals(DEPARTMENT, f.roomDepartment);
    }

    @Test
    void queueUsesRequestedDateAndQueryFailureDoesNotWriteSuccessfulAudit() {
        Fixture f = new Fixture();
        f.permissions = List.of(grant("RECEPTION"));
        LocalDate requestedDate = LocalDate.of(2026, 11, 5);

        KetQua<TiepDonHangDoiAnhChup> result = f.queueHandler().handle(
                new GetTiepDonHangDoiQuery(requestedDate, f.context, f.actor));

        assertTrue(result.isSuccess());
        assertEquals(requestedDate, f.queueDate);
        assertEquals(requestedDate, f.summaryDate);
        assertEquals(List.of("queue", "summaries", "audit"), f.order);

        Fixture defaultDateFixture = new Fixture();
        defaultDateFixture.permissions = List.of(grant("RECEPTION"));
        LocalDate before = LocalDate.now();
        KetQua<TiepDonHangDoiAnhChup> defaultDate = defaultDateFixture.queueHandler().handle(
                new GetTiepDonHangDoiQuery(null, defaultDateFixture.context, defaultDateFixture.actor));
        LocalDate after = LocalDate.now();
        assertTrue(defaultDate.isSuccess());
        assertTrue(!defaultDateFixture.queueDate.isBefore(before) && !defaultDateFixture.queueDate.isAfter(after));
        assertEquals(defaultDateFixture.queueDate, defaultDateFixture.summaryDate);

        f = new Fixture();
        f.permissions = List.of(grant("RECEPTION"));
        f.queueFailure = new IllegalStateException("synthetic queue failure");

        KetQua<TiepDonHangDoiAnhChup> failed = f.queueHandler().handle(f.queueQuery());

        assertTrue(failed.isFailure());
        assertTrue(failed.getValue().isEmpty());
        assertTrue(f.audit.entries.isEmpty());
    }

    private static BenhNhan patient(int id) {
        BenhNhan patient = new BenhNhan();
        patient.setId(id);
        patient.setMaBenhNhanFormat("BN-SYN-" + id);
        patient.setHo("Họ Tổng Hợp");
        patient.setTen("Người Bệnh");
        return patient;
    }

    private static MaTranPhanQuyen grant(String module) {
        MaTranPhanQuyen permission = new MaTranPhanQuyen();
        permission.setMaVaiTro(ROLE);
        permission.setPhanHe(module);
        permission.setHanhDong(HanhDongQuyen.XEM);
        permission.setDuocPhep(true);
        permission.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        return permission;
    }

    private static final class Fixture {
        final PhanQuyenNguCanh context = new PhanQuyenNguCanh(EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT, SESSION);
        final KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT,
                "synthetic-workstation", UUID.randomUUID());
        final List<String> order = new ArrayList<>();
        final TestAudit audit = new TestAudit(order);
        List<MaTranPhanQuyen> permissions = List.of(grant("PATIENT"), grant("RECEPTION"), grant("CATALOG"));
        List<BenhNhan> patientResults = List.of();
        final Map<Integer, BaoHiem> cardsByPatient = new HashMap<>();
        RuntimeException queryFailure;
        RuntimeException insuranceFailure;
        int patientQueries, insuranceLookups;
        int requestedLimit;
        String lastKeyword;
        int queueFacility, queueDepartment, roomFacility, roomDepartment;
        int summaryFacility, summaryDepartment;
        LocalDate queueDate, summaryDate;
        RuntimeException queueFailure;
        boolean sessionActive = true, assignmentValid = true;

        final IBenhNhanRepository patients = (IBenhNhanRepository) Proxy.newProxyInstance(
                IBenhNhanRepository.class.getClassLoader(), new Class<?>[]{IBenhNhanRepository.class}, (p, m, a) -> {
                    if (m.getName().equals("searchInScope")) {
                        order.add("query"); patientQueries++; lastKeyword = (String) a[0]; requestedLimit = (int) a[3];
                        if (queryFailure != null) throw queryFailure;
                        return patientResults;
                    }
                    return defaultValue(m.getReturnType());
                });
        final IBaoHiemRepository insurance = (IBaoHiemRepository) Proxy.newProxyInstance(
                IBaoHiemRepository.class.getClassLoader(), new Class<?>[]{IBaoHiemRepository.class}, (p, m, a) -> {
                    if (m.getName().equals("getLatestByPatientId")) {
                        insuranceLookups++;
                        if (insuranceFailure != null) throw insuranceFailure;
                        return Optional.ofNullable(cardsByPatient.get((int) a[0]));
                    }
                    return defaultValue(m.getReturnType());
                });
        final TiepDonHangDoiCongKetNoi queue = new TiepDonHangDoiCongKetNoi() {
            @Override public vn.clinicmanagement.domain.examination.LuotKham registerVisit(int p, int f, int d, int r,
                    String t, String reason, vn.clinicmanagement.domain.common.enums.DoiTuongKham payer, boolean priority, int by) { return null; }
            @Override public List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facility, int department) {
                order.add("queue"); queueFacility = facility; queueDepartment = department; queueDate = date;
                if (queueFailure != null) throw queueFailure;
                return List.of();
            }
            @Override public List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facility, int department) {
                order.add("summaries"); summaryFacility = facility; summaryDepartment = department;
                summaryDate = date; return List.of();
            }
        };
        final IPhongKhamRepository rooms = (IPhongKhamRepository) Proxy.newProxyInstance(
                IPhongKhamRepository.class.getClassLoader(), new Class<?>[]{IPhongKhamRepository.class}, (p, m, a) -> {
                    if (m.getName().equals("getActiveRoomsInScope")) {
                        order.add("rooms"); roomFacility = (int) a[0]; roomDepartment = (int) a[1]; return List.of();
                    }
                    return defaultValue(m.getReturnType());
                });
        final PhanQuyenDichVu authorization;

        Fixture() {
            NhanVien employee = new NhanVien(); employee.setId(EMPLOYEE);
            VaiTro role = new VaiTro(); role.setId(ROLE); role.setTenVaiTro(ROLE_CODE);
            INhanVienRepository employeeRepository = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class}, (p, m, a) -> switch (m.getName()) {
                        case "getById" -> Optional.of(employee);
                        case "getAssignedRoles" -> List.of(role);
                        case "getAssignedWorkScopes" -> assignmentValid
                                ? List.of(new NhanVienWorkPhamVi(FACILITY, "Synthetic facility", DEPARTMENT, "Synthetic department"))
                                : List.of();
                        default -> defaultValue(m.getReturnType());
                    });
            IHeThongRepository system = new IHeThongRepository() {
                @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int id) { return permissions; }
                @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
            };
            authorization = new PhanQuyenDichVu(id -> Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(
                    SESSION, EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT, sessionActive, true, true)), employeeRepository, system);
        }

        KetQua<List<BenhNhanSearchMatHang>> search(String keyword) { return search(keyword, context); }
        KetQua<List<BenhNhanSearchMatHang>> search(String keyword, PhanQuyenNguCanh auth) {
            return new SearchBenhNhanHandler(patients, insurance, authorization, audit)
                    .handle(new SearchBenhNhanQuery(keyword, auth, actor));
        }
        GetTiepDonHangDoiHandler queueHandler() { return new GetTiepDonHangDoiHandler(queue, authorization, audit); }
        GetTiepDonHangDoiQuery queueQuery() { return new GetTiepDonHangDoiQuery(LocalDate.of(2026, 11, 5), context, actor); }
        GetHoatDongTiepDonPhongHandler roomHandler() { return new GetHoatDongTiepDonPhongHandler(rooms, authorization, audit); }
        GetHoatDongTiepDonPhongQuery roomQuery() { return new GetHoatDongTiepDonPhongQuery(context, actor); }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static final class TestAudit implements KiemToanWriteCongKetNoi {
        final List<String> order;
        final List<HeThongKiemToanNhapLieu> entries = new ArrayList<>();
        RuntimeException failure;
        TestAudit(List<String> order) { this.order = order; }
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            order.add("audit");
            if (failure != null) throw failure;
            entries.add(entry);
        }
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) { throw new UnsupportedOperationException(); }
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) { throw new UnsupportedOperationException(); }
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) { throw new UnsupportedOperationException(); }
    }
}

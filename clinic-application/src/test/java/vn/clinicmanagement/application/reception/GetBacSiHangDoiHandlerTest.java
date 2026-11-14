package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetBacSiHangDoiHandlerTest {
    private static final int EMPLOYEE = 17;
    private static final int ROLE = 4;
    private static final int FACILITY = 3;
    private static final int DEPARTMENT = 8;
    private static final String ROLE_CODE = "DOCTOR";
    private static final UUID SESSION = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");

    @Test
    void requiresValidCurrentContextsAndExistingReceptionViewGrant() {
        Fixture fixture = new Fixture();
        fixture.permissions = List.of(grant("PATIENT"));

        KetQua<List<TiepDonHangDoiMatHang>> denied = fixture.handler().handle(fixture.query());

        assertTrue(denied.isFailure());
        assertTrue(denied.getValue().isEmpty());
        assertEquals(0, fixture.queueQueries);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entries.get(0).actionCode());

        fixture = new Fixture();
        KetQua<List<TiepDonHangDoiMatHang>> missingContext = fixture.handler().handle(
                new GetBacSiHangDoiQuery(null, null, null));
        assertTrue(missingContext.isFailure());
        assertEquals(0, fixture.queueQueries);
        assertTrue(fixture.audit.entries.isEmpty());

        fixture = new Fixture();
        PhanQuyenNguCanh mismatchedScope = new PhanQuyenNguCanh(
                EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT + 1, SESSION);
        KetQua<List<TiepDonHangDoiMatHang>> stale = fixture.handler().handle(
                new GetBacSiHangDoiQuery(null, mismatchedScope, fixture.actor));
        assertTrue(stale.isFailure());
        assertEquals(0, fixture.queueQueries);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entries.get(0).actionCode());
    }

    @Test
    void queriesExactTeamScopeAndRequestedDateBeforeQueueLevelAudit() {
        Fixture fixture = new Fixture();
        TiepDonHangDoiMatHang item = new TiepDonHangDoiMatHang(51, 72, "BN-SYN-72", "Người bệnh tổng hợp",
                91, "Phòng tổng hợp", 5, "05", "CHO_KHAM", LocalDateTime.of(2026, 11, 6, 8, 30));
        fixture.items = List.of(item);
        LocalDate date = LocalDate.of(2026, 11, 6);

        KetQua<List<TiepDonHangDoiMatHang>> result = fixture.handler().handle(
                new GetBacSiHangDoiQuery(date, fixture.context, fixture.actor));

        assertTrue(result.isSuccess());
        assertEquals(List.of(item), result.valueOrThrow());
        assertEquals(List.of("queue", "audit"), fixture.order);
        assertEquals(date, fixture.requestedDate);
        assertEquals(FACILITY, fixture.requestedFacility);
        assertEquals(DEPARTMENT, fixture.requestedDepartment);
        HeThongKiemToanNhapLieu entry = fixture.audit.entries.get(0);
        assertEquals(KiemToanHanhDongMa.PATIENT_VIEW, entry.actionCode());
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
        assertEquals(EMPLOYEE, entry.actorEmployeeId());
        assertEquals(FACILITY, entry.facilityId());
        assertEquals(DEPARTMENT, entry.departmentId());
    }

    @Test
    void defaultsToCurrentDateAndFailsClosedWhenIndependentAuditFails() {
        Fixture fixture = new Fixture();
        fixture.audit.failure = new IllegalStateException("synthetic audit failure");

        KetQua<List<TiepDonHangDoiMatHang>> result = fixture.handler().handle(
                new GetBacSiHangDoiQuery(null, fixture.context, fixture.actor));

        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertEquals(List.of("queue", "audit"), fixture.order);
        assertEquals(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), fixture.requestedDate);
        assertTrue(fixture.audit.entries.isEmpty());
    }

    @Test
    void queryFailureDoesNotWriteSuccessfulReadAudit() {
        Fixture fixture = new Fixture();
        fixture.queueFailure = new IllegalStateException("synthetic query failure");

        KetQua<List<TiepDonHangDoiMatHang>> result = fixture.handler().handle(fixture.query());

        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertEquals(List.of("queue"), fixture.order);
        assertTrue(fixture.audit.entries.isEmpty());
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
        final PhanQuyenNguCanh context = new PhanQuyenNguCanh(
                EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT, SESSION);
        final KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT,
                "synthetic-workstation", UUID.randomUUID());
        final List<String> order = new ArrayList<>();
        final TestAudit audit = new TestAudit(order);
        List<MaTranPhanQuyen> permissions = List.of(grant("RECEPTION"));
        List<TiepDonHangDoiMatHang> items = List.of();
        RuntimeException queueFailure;
        int queueQueries;
        int requestedFacility;
        int requestedDepartment;
        LocalDate requestedDate;

        final BacSiHangDoiCongKetNoi queue = (date, facilityId, departmentId) -> {
            order.add("queue");
            queueQueries++;
            requestedDate = date;
            requestedFacility = facilityId;
            requestedDepartment = departmentId;
            if (queueFailure != null) throw queueFailure;
            return items;
        };
        final PhanQuyenDichVu authorization;

        Fixture() {
            NhanVien employee = new NhanVien();
            employee.setId(EMPLOYEE);
            VaiTro role = new VaiTro();
            role.setId(ROLE);
            role.setTenVaiTro(ROLE_CODE);
            INhanVienRepository employees = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> Optional.of(employee);
                        case "getAssignedRoles" -> List.of(role);
                        case "getAssignedWorkScopes" -> List.of(new NhanVienWorkPhamVi(
                                FACILITY, "Synthetic facility", DEPARTMENT, "Synthetic department"));
                        default -> defaultValue(method.getReturnType());
                    });
            IHeThongRepository system = new IHeThongRepository() {
                @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return permissions; }
                @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
            };
            authorization = new PhanQuyenDichVu(
                    sessionId -> Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(
                            SESSION, EMPLOYEE, ROLE, ROLE_CODE, FACILITY, DEPARTMENT, true, true, true)),
                    employees, system);
        }

        GetBacSiHangDoiHandler handler() { return new GetBacSiHangDoiHandler(queue, authorization, audit); }
        GetBacSiHangDoiQuery query() { return new GetBacSiHangDoiQuery(null, context, actor); }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
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
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
    }
}

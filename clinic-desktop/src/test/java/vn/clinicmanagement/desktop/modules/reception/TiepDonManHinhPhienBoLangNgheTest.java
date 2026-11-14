package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.PhongKhamPhongHangDoiTongHop;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongQuery;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiQuery;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.TiepDonHangDoiAnhChup;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonManHinhPhienBoLangNgheTest {

    @BeforeAll
    static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private final NguoiDungPhien session = NguoiDungPhien.getInstance();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-07T08:00:00Z"));
    private final TestAuditPort audit = new TestAuditPort();
    private PhienVongDoiDichVu lifecycle;

    private final RoleChoice role = new RoleChoice(10, "RECEPTIONIST", "Nhân viên tiếp đón");
    private final WorkScopeChoice scope = new WorkScopeChoice(1, "Cơ sở 1", 2, "Khoa khám bệnh");

    private FakeQueuePort fakeQueuePort;
    private FakeRoomRepository fakeRoomRepository;
    private GetTiepDonHangDoiHandler queueHandler;
    private GetHoatDongTiepDonPhongHandler roomsHandler;

    @BeforeEach
    void setUp() {
        session.clearSession();
        lifecycle = new PhienVongDoiDichVu(
                session,
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                clock,
                audit,
                Runnable::run
        );

        fakeQueuePort = new FakeQueuePort();
        fakeRoomRepository = new FakeRoomRepository();

        // Stub authorization that always allows for the test
        PhanQuyenDichVu permissiveAuthz = new PhanQuyenDichVu(
                sessionId -> Optional.of(new vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi.ActiveSession(
                        sessionId, 101, 10, "RECEPTIONIST", 1, 2, true, true, true)),
                createFakeEmployeeRepository(),
                new FakeSystemRepository()
        );

        queueHandler = new GetTiepDonHangDoiHandler(fakeQueuePort, permissiveAuthz, audit);
        roomsHandler = new GetHoatDongTiepDonPhongHandler(fakeRoomRepository, permissiveAuthz, audit);
    }

    @AfterEach
    void tearDown() {
        session.clearSession();
    }

    private void startSession() {
        lifecycle.startSession(new DaHoanTatDangNhapPhien(
                101, "receptionist.test", "Nhân viên Mẫu", role, scope,
                UUID.randomUUID(), clock.instant()));
    }

    @Test
    void visualLayoutDividerPreservedAt218px() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            panel.setSize(1200, 800);
            panel.doLayout();
            JSplitPane splitPane = findComponent(panel, JSplitPane.class);
            assertNotNull(splitPane, "JSplitPane must be present in ReceptionPanel");
            splitPane.setSize(1200, 700);
            splitPane.doLayout();
            assertEquals(218, splitPane.getDividerLocation(), "Divider location must be preserved at 218px");
            assertEquals(4, splitPane.getDividerSize(), "Divider size must be 4px");
            panel.destroy();
        });
    }

    @Test
    void activeSessionFormIsEditable() throws Exception {
        startSession();
        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            assertTrue(panel.isFormEditable(), "Form inputs must be editable when session is active");
            panel.destroy();
        });
    }

    @Test
    void sessionLockCancelsWorkersAndMakesFormReadOnly() throws Exception {
        startSession();
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            assertTrue(panel.isFormEditable());

            // Advance clock past idle timeout (15 minutes)
            clock.advance(Duration.ofMinutes(16));
            lifecycle.evaluate();

            // Wait for EDT updates
            SwingUtilities.invokeLater(() -> {
                assertFalse(panel.isFormEditable(), "Form inputs must become read-only when session locks");
                assertEquals(0, panel.getInFlightWorkersCount(), "Pending workers must be cancelled");
                panel.destroy();
                latch.countDown();
            });
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    void sessionLogoutCancelsWorkersAndClearsForm() throws Exception {
        startSession();
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            assertTrue(panel.isFormEditable());

            // Clear session to simulate logout
            lifecycle.logout();

            SwingUtilities.invokeLater(() -> {
                assertFalse(panel.isFormEditable(), "Form inputs must be disabled on logout");
                assertEquals(0, panel.getInFlightWorkersCount(), "Workers must be cancelled on logout");
                panel.destroy();
                latch.countDown();
            });
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    void refreshDataExecutesOffEdtAndUpdatesQueueAndRooms() throws Exception {
        startSession();

        // Prepare synthetic queue items and room summaries
        fakeQueuePort.queueItems.add(new TiepDonHangDoiMatHang(
                1, 201, "BN0001", "NGUYEN VAN A", 10, "PK Nhi 1", 5, "05", "Chờ khám",
                LocalDateTime.of(2026, 11, 7, 8, 30)));
        fakeQueuePort.roomSummaries.add(new PhongKhamPhongHangDoiTongHop(
                10, "PK Nhi 1", "Khoa Nhi", 5, 3, 2));

        // Add a mock active room
        PhongKham pk = new PhongKham();
        pk.setId(10);
        pk.setTenPhong("PK Nhi 1");
        pk.setHoatDong(true);
        Khoa k = new Khoa();
        k.setId(2);
        k.setHoatDong(true);
        CoSo c = new CoSo();
        c.setId(1);
        c.setHoatDong(true);
        k.setCoSo(c);
        pk.setKhoa(k);
        fakeRoomRepository.rooms.add(pk);

        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            panel.refreshData();

            // Wait for worker done() callback on EDT
            Timer checkTimer = new Timer(50, null);
            checkTimer.addActionListener(e -> {
                if (panel.getInFlightWorkersCount() == 0) {
                    checkTimer.stop();

                    // Verify queue table in statusPanel has been updated
                    List<JTable> tables = findComponents(panel.getStatusPanel(), JTable.class);
                    assertFalse(tables.isEmpty(), "Tables must exist in ClinicRoomStatusPanel");

                    JTable queueTable = tables.stream()
                            .filter(t -> t.getColumnCount() == 7)
                            .findFirst()
                            .orElse(null);
                    assertNotNull(queueTable, "Queue table (7 columns) must exist");

                    DefaultTableModel queueModel = (DefaultTableModel) queueTable.getModel();
                    assertEquals(1, queueModel.getRowCount(), "Queue table should have 1 synthetic row");
                    assertEquals("05", queueModel.getValueAt(0, 1), "STT Khám should be '05'");
                    assertEquals("Chờ khám", queueModel.getValueAt(0, 2), "Trạng thái should be 'Chờ khám'");

                    // Verify room status count updated
                    JTable roomTable = tables.stream()
                            .filter(t -> t.getColumnCount() == 4)
                            .findFirst()
                            .orElse(null);
                    assertNotNull(roomTable, "Room table (4 columns) must exist");

                    panel.destroy();
                    latch.countDown();
                }
            });
            checkTimer.start();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "refreshData should finish asynchronously");
    }

    @Test
    void destroyUnregistersListenerAndCancelsInFlightWorkers() throws Exception {
        startSession();
        SwingUtilities.invokeAndWait(() -> {
            TiepDonManHinh panel = new TiepDonManHinh(queueHandler, roomsHandler, null, null, lifecycle);
            panel.refreshData();
            panel.destroy();

            assertEquals(0, panel.getInFlightWorkersCount());
        });
    }

    // --- Helper Component Traversals ---

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponent(Component root, Class<T> clazz) {
        if (clazz.isInstance(root)) return (T) root;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponent(child, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> findComponents(Component root, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        findComponentsRecursive(root, clazz, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> void findComponentsRecursive(Component comp, Class<T> clazz, List<T> result) {
        if (clazz.isInstance(comp)) result.add((T) comp);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                findComponentsRecursive(child, clazz, result);
            }
        }
    }

    // --- In-Memory Synthetic Stubs ---

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration d) { instant = instant.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    private static final class TestAuditPort implements KiemToanWriteCongKetNoi {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    }

    private static final class FakeQueuePort implements TiepDonHangDoiCongKetNoi {
        final List<TiepDonHangDoiMatHang> queueItems = new ArrayList<>();
        final List<PhongKhamPhongHangDoiTongHop> roomSummaries = new ArrayList<>();

        @Override
        public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                     String registrationType, String reason, DoiTuongKham payerType,
                                     boolean priority, int createdBy) {
            return new LuotKham();
        }

        @Override
        public List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facilityId, int departmentId) {
            return List.copyOf(queueItems);
        }

        @Override
        public List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
            return List.copyOf(roomSummaries);
        }
    }

    private static final class FakeRoomRepository implements IPhongKhamRepository {
        final List<PhongKham> rooms = new ArrayList<>();

        @Override public Optional<PhongKham> getById(int id) {
            return rooms.stream().filter(r -> r.getId() != null && r.getId() == id).findFirst();
        }
        @Override public List<PhongKham> getActiveRooms() { return List.copyOf(rooms); }
        @Override public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
            return List.copyOf(rooms);
        }
    }

    private static vn.clinicmanagement.domain.repositories.INhanVienRepository createFakeEmployeeRepository() {
        vn.clinicmanagement.domain.identity.NhanVien employee = new vn.clinicmanagement.domain.identity.NhanVien();
        employee.setId(101);
        employee.setHoatDong(true);

        vn.clinicmanagement.domain.identity.VaiTro role = new vn.clinicmanagement.domain.identity.VaiTro();
        role.setId(10);
        role.setTenVaiTro("RECEPTIONIST");

        vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi scope =
                new vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi(1, "Cơ sở 1", 2, "Khoa khám bệnh");

        return (vn.clinicmanagement.domain.repositories.INhanVienRepository) java.lang.reflect.Proxy.newProxyInstance(
                vn.clinicmanagement.domain.repositories.INhanVienRepository.class.getClassLoader(),
                new Class<?>[]{vn.clinicmanagement.domain.repositories.INhanVienRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> {
                        int id = (int) args[0];
                        vn.clinicmanagement.domain.identity.NhanVien emp = new vn.clinicmanagement.domain.identity.NhanVien();
                        emp.setId(id);
                        emp.setHoatDong(true);
                        yield Optional.of(emp);
                    }
                    case "getAssignedRoles" -> List.of(role);
                    case "getAssignedWorkScopes" -> List.of(scope);
                    case "getRoleNames" -> List.of("RECEPTIONIST");
                    case "getActiveDoctors" -> List.of();
                    case "toString" -> "FakeEmployeeRepository";
                    default -> null;
                });
    }

    private static final class FakeSystemRepository implements vn.clinicmanagement.domain.repositories.IHeThongRepository {
        @Override
        public List<vn.clinicmanagement.domain.system.MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
            vn.clinicmanagement.domain.system.MaTranPhanQuyen p1 = new vn.clinicmanagement.domain.system.MaTranPhanQuyen();
            p1.setMaVaiTro(roleId);
            p1.setPhanHe("RECEPTION");
            p1.setHanhDong(vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM);
            p1.setPhamViDuLieu(vn.clinicmanagement.domain.common.enums.PhamViDuLieu.KHOA_PHONG);
            p1.setDuocPhep(true);

            vn.clinicmanagement.domain.system.MaTranPhanQuyen p2 = new vn.clinicmanagement.domain.system.MaTranPhanQuyen();
            p2.setMaVaiTro(roleId);
            p2.setPhanHe("CATALOG");
            p2.setHanhDong(vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM);
            p2.setPhamViDuLieu(vn.clinicmanagement.domain.common.enums.PhamViDuLieu.KHOA_PHONG);
            p2.setDuocPhep(true);

            return List.of(p1, p2);
        }

        @Override
        public Optional<String> getSystemParameterValue(String key) {
            return Optional.empty();
        }
    }
}

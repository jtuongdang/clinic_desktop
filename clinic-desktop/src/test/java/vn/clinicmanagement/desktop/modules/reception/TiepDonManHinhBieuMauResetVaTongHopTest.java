package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.reception.PhongKhamPhongHangDoiTongHop;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.RegisterBenhNhanCommand;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.reception.components.YTeBatLenBoChon;
import vn.clinicmanagement.desktop.modules.reception.components.BenhNhanDangKyBieuMauManHinh;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonManHinhBieuMauResetVaTongHopTest {
    private static final RoleChoice ROLE = new RoleChoice(10, "RECEPTIONIST", "Tiếp đón mẫu");
    private static final WorkScopeChoice SCOPE = new WorkScopeChoice(1, "Cơ sở mẫu", 2, "Khoa mẫu");
    private static final int EMPLOYEE_ID = 101;
    private static final Clock SESSION_CLOCK = Clock.fixed(Instant.parse("2026-11-07T08:00:00Z"), ZoneOffset.UTC);

    private final NguoiDungPhien session = NguoiDungPhien.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final SyntheticQueue queue = new SyntheticQueue();
    private volatile boolean failRoomCatalog;
    private PhienVongDoiDichVu lifecycle;
    private TiepDonManHinh panel;

    @BeforeAll
    static void configureHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        session.clearSession();
        KiemToanWriteCongKetNoi audit = new KiemToanWriteCongKetNoi() {
            @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
            @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
        };
        lifecycle = new PhienVongDoiDichVu(session,
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                SESSION_CLOCK, audit, Runnable::run);
        lifecycle.startSession(new DaHoanTatDangNhapPhien(EMPLOYEE_ID, "reception.test", "Nhân viên mẫu",
                ROLE, SCOPE, sessionId, SESSION_CLOCK.instant()));
        PhanQuyenDichVu authorization = syntheticAuthorization(lifecycle);
        GetTiepDonHangDoiHandler queueHandler = new GetTiepDonHangDoiHandler(queue, authorization, audit);
        GetHoatDongTiepDonPhongHandler roomsHandler = new GetHoatDongTiepDonPhongHandler(
                syntheticRoomsRepository(), authorization, audit);
        SwingUtilities.invokeAndWait(() -> panel = new TiepDonManHinh(
                queueHandler, roomsHandler, null, null, lifecycle));
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (panel != null) SwingUtilities.invokeAndWait(panel::destroy);
        } finally {
            session.clearSession();
        }
    }

    @Test
    void toolbarNewClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            clickNew(panel.getTopToolbar());
            assertFormReset();
        });
    }

    @Test
    void footerNewClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            clickNew(((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
            assertFormReset();
        });
    }

    @Test
    void logoutClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            assertTrue(panel.isFormEditable());
            lifecycle.logout();
            assertFormReset();
            assertFalse(panel.isFormEditable());
        });
    }

    @Test
    void refreshPopulatesBothEmptyRoomTablesAndClearsThemOnEmptySnapshot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, roomTables().size());
            roomTables().forEach(table -> assertEquals(0, table.getRowCount()));
        });

        queue.rooms = List.of(
                new PhongKhamPhongHangDoiTongHop(71, "Phòng mẫu A", "Khoa mẫu", 8, 5, 2),
                new PhongKhamPhongHangDoiTongHop(72, "Phòng mẫu B", "Khoa mẫu", 6, 3, 1));
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> {
            List<JTable> tables = roomTables();
            assertRoomRow(tables.get(0), "Phòng mẫu A", 8, 5, 2);
            assertRoomRow(tables.get(1), "Phòng mẫu B", 6, 3, 1);
        });

        queue.rooms = List.of();
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> roomTables().forEach(table -> assertEquals(0, table.getRowCount())));
    }

    @Test
    void failedQueueRefreshClearsPreviouslyDisplayedRoomSummaries() throws Exception {
        queue.rooms = List.of(
                new PhongKhamPhongHangDoiTongHop(71, "Phòng mẫu A", "Khoa mẫu", 8, 5, 2),
                new PhongKhamPhongHangDoiTongHop(72, "Phòng mẫu B", "Khoa mẫu", 6, 3, 1));
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, roomTables().get(0).getRowCount());
            assertEquals(1, roomTables().get(1).getRowCount());
        });

        queue.failQueue = true;
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, roomTables().get(0).getRowCount());
            assertEquals(0, roomTables().get(1).getRowCount());
            assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_RECEPTION_QUEUE_FAILED, panel.getLastErrorMessage());
        });
    }

    @Test
    void refreshReportsSafeMessageWhenQueueResultFails() throws Exception {
        queue.failQueue = true;
        refreshAndWait();
        SwingUtilities.invokeAndWait(() ->
                assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_RECEPTION_QUEUE_FAILED, panel.getLastErrorMessage()));
    }

    @Test
    void refreshReportsSafeMessageWhenRoomsResultFails() throws Exception {
        failRoomCatalog = true;
        refreshAndWait();
        SwingUtilities.invokeAndWait(() ->
                assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_ROOM_CATALOG_FAILED, panel.getLastErrorMessage()));
    }

    @Test
    void refreshQueriesTheClinicDateAcrossMidnightWithUtcJvmAndClock() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertQueryDate("2026-11-07T16:59:59Z", LocalDate.of(2026, 11, 7));
            assertQueryDate("2026-11-07T17:00:00Z", LocalDate.of(2026, 11, 8));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private void selectPatientAndChangeVisitDefaults() {
        PhongKham defaultRoom = new PhongKham();
        defaultRoom.setId(71);
        defaultRoom.setTenPhong("Phòng mẫu A");
        PhongKham otherRoom = new PhongKham();
        otherRoom.setId(72);
        otherRoom.setTenPhong("Phòng mẫu B");
        panel.updateActiveRooms(List.of(defaultRoom, otherRoom));

        BenhNhan patient = new BenhNhan();
        patient.setId(51);
        patient.setTen("Người bệnh mẫu");
        patient.setGioiTinh(GioiTinh.M);
        patient.setDanToc("Dân tộc mẫu");
        patient.setQuocTich("Quốc tịch mẫu");
        patient.setDiaChi("Địa chỉ mẫu");
        BaoHiem insurance = new BaoHiem();
        insurance.setMucHuong(80);
        insurance.setDiaChiThe("Địa chỉ thẻ mẫu");

        BenhNhanDangKyBieuMauManHinh form = panel.getFormPanel();
        form.selectPatient(patient, insurance);
        comboStartingWith("Khám bệnh").setSelectedItem("Tái khám");
        comboStartingWith("Viện phí").setSelectedItem("BHYT");
        components(form, YTeBatLenBoChon.class).stream()
                .filter(chooser -> defaultRoom.getTenPhong().equals(chooser.getText()))
                .findFirst().orElseThrow().setText(otherRoom.getTenPhong());
        RegisterBenhNhanCommand selected = formCommand();
        assertEquals(51, selected.patient().getId());
        assertEquals(72, selected.roomId());
        assertEquals("Tái khám", selected.registrationType());
        assertEquals(DoiTuongKham.BHYT, selected.payerType());
        assertEquals(80, selected.insurance().getMucHuong());
    }

    private void assertFormReset() {
        RegisterBenhNhanCommand cleared = formCommand();
        assertNull(cleared.patient().getId());
        assertEquals("", cleared.patient().getHoTen());
        assertEquals("", cleared.patient().getDiaChi());
        assertEquals(GioiTinh.F, cleared.patient().getGioiTinh());
        assertEquals("Kinh", cleared.patient().getDanToc());
        assertEquals("Việt nam", cleared.patient().getQuocTich());
        assertEquals(5, comboStartingWith("Kinh").getItemCount());
        assertEquals(2, comboStartingWith("Việt nam").getItemCount());
        assertNull(cleared.insurance());
        assertEquals(71, cleared.roomId());
        assertEquals("Khám bệnh", cleared.registrationType());
        assertEquals(DoiTuongKham.THU_PHI, cleared.payerType());
        assertTrue(components(panel.getFormPanel(), JTextField.class).stream()
                .anyMatch(field -> field.getText().matches("\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4}")),
                "The reset should restore the registration timestamp");
    }

    private RegisterBenhNhanCommand formCommand() {
        return panel.getFormPanel().toRegisterCommand(
                new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE.id(), ROLE.code(),
                        SCOPE.facilityId(), SCOPE.departmentId(), sessionId),
                new KiemToanTacNhanNguCanh(EMPLOYEE_ID, ROLE.id(), ROLE.code(),
                        SCOPE.facilityId(), SCOPE.departmentId(), "SYNTHETIC-01", UUID.randomUUID()));
    }

    private JComboBox<?> comboStartingWith(String firstItem) {
        return components(panel.getFormPanel(), JComboBox.class).stream()
                .filter(combo -> firstItem.equals(combo.getItemAt(0)))
                .findFirst().orElseThrow();
    }

    private static void clickNew(Component bar) {
        components(bar, JButton.class).stream()
                .filter(button -> button.getText() != null && button.getText().contains("Bệnh Nhân Mới"))
                .findFirst().orElseThrow().doClick(0);
    }

    private List<JTable> roomTables() {
        // Only the first two room grids display summaries. The third grid is decorative.
        return components(panel.getStatusPanel(), JTable.class).stream()
                .filter(table -> table.getColumnCount() == 4)
                .limit(2)
                .toList();
    }

    private static void assertRoomRow(JTable table, String name, int registered, int waiting, int examining) {
        assertEquals(1, table.getRowCount());
        assertEquals(name, table.getValueAt(0, 0));
        assertEquals(registered, table.getValueAt(0, 1));
        assertEquals(waiting, table.getValueAt(0, 2));
        assertEquals(examining, table.getValueAt(0, 3));
    }

    private void assertQueryDate(String instant, LocalDate expected) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.setQueueClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)));
        refreshAndWait();
        assertEquals(expected, queue.queueDate);
        assertEquals(expected, queue.summaryDate);
    }

    private void refreshAndWait() throws Exception {
        SwingUtilities.invokeAndWait(panel::refreshData);
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (panel.getInFlightWorkersCount() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(0, panel.getInFlightWorkersCount(), "Refresh worker did not finish");
        SwingUtilities.invokeAndWait(() -> {});
    }

    private static <T extends Component> List<T> components(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) matches.add(type.cast(root));
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                matches.addAll(components(child, type));
            }
        }
        return matches;
    }

    private static PhanQuyenDichVu syntheticAuthorization(PhienVongDoiDichVu lifecycle) {
        NhanVien employee = new NhanVien();
        employee.setId(EMPLOYEE_ID);
        employee.setHoatDong(true);
        VaiTro role = new VaiTro();
        role.setId(ROLE.id());
        role.setTenVaiTro(ROLE.code());
        NhanVienWorkPhamVi scope = new NhanVienWorkPhamVi(
                SCOPE.facilityId(), SCOPE.facilityName(), SCOPE.departmentId(), SCOPE.departmentName());
        INhanVienRepository employees = (INhanVienRepository) Proxy.newProxyInstance(
                INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> Optional.of(employee);
                    case "getAssignedRoles" -> List.of(role);
                    case "getAssignedWorkScopes" -> List.of(scope);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        MaTranPhanQuyen grant = new MaTranPhanQuyen();
        grant.setMaVaiTro(ROLE.id());
        grant.setPhanHe("RECEPTION");
        grant.setHanhDong(HanhDongQuyen.XEM);
        grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        grant.setDuocPhep(true);
        MaTranPhanQuyen catalogGrant = new MaTranPhanQuyen();
        catalogGrant.setMaVaiTro(ROLE.id());
        catalogGrant.setPhanHe("CATALOG");
        catalogGrant.setHanhDong(HanhDongQuyen.XEM);
        catalogGrant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        catalogGrant.setDuocPhep(true);
        IHeThongRepository system = new IHeThongRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                return List.of(grant, catalogGrant);
            }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };
        return new PhanQuyenDichVu(lifecycle, employees, system);
    }

    private IPhongKhamRepository syntheticRoomsRepository() {
        return (IPhongKhamRepository) Proxy.newProxyInstance(
                IPhongKhamRepository.class.getClassLoader(), new Class<?>[]{IPhongKhamRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getActiveRoomsInScope" -> {
                        if (failRoomCatalog) throw new IllegalStateException("Synthetic room catalog failure");
                        yield List.of();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class SyntheticQueue implements TiepDonHangDoiCongKetNoi {
        private volatile List<PhongKhamPhongHangDoiTongHop> rooms = List.of();
        private volatile LocalDate queueDate;
        private volatile LocalDate summaryDate;
        private volatile boolean failQueue;

        @Override
        public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                      String registrationType, String reason, DoiTuongKham payerType,
                                      boolean priority, int createdBy) {
            throw new UnsupportedOperationException("Refresh tests only read the synthetic queue");
        }

        @Override
        public List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facilityId, int departmentId) {
            if (failQueue) throw new IllegalStateException("Synthetic reception queue failure");
            queueDate = date;
            return List.of();
        }

        @Override
        public List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
            summaryDate = date;
            return rooms;
        }
    }
}

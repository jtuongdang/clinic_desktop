package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.patienthistory.GetBenhNhanTienSuHandler;
import vn.clinicmanagement.application.reception.BacSiHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.GetBacSiHangDoiHandler;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.doctor.model.BenhNhanKhamDuLieu;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacSiTramKetNoiTest {

    @AfterEach
    void tearDown() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    @DisplayName("DoctorStationPanel initializes cleanly without hardcoded TEST BN 060 / TEST BN 033 sample data")
    void initializesWithoutHardcodedSampleData() {
        BacSiTramManHinh panel = new BacSiTramManHinh();
        assertNotNull(panel);

        List<JLabel> labels = descendantsOfType(panel, JLabel.class);
        for (JLabel lbl : labels) {
            String text = lbl.getText();
            if (text != null) {
                assertFalse(text.contains("TEST BN 060"), "Must not contain TEST BN 060: " + text);
                assertFalse(text.contains("TEST BN 033"), "Must not contain TEST BN 033: " + text);
            }
        }
    }

    @Test
    @DisplayName("Loads real doctor queue data and updates UI components dynamically")
    void loadsRealDoctorQueueAndUpdatesComponents() throws Exception {
        List<TiepDonHangDoiMatHang> syntheticQueue = List.of(
                new TiepDonHangDoiMatHang(10, 101, "BN-2026-001", "Nguyễn Văn Thực",
                        1, "Phòng Khám Nội 1", 1, "01", "Chờ khám", LocalDateTime.of(2026, 11, 7, 8, 30)),
                new TiepDonHangDoiMatHang(11, 102, "BN-2026-002", "Trần Thị Thật",
                        1, "Phòng Khám Nội 1", 2, "02", "Chờ khám", LocalDateTime.of(2026, 11, 7, 8, 45))
        );

        BacSiHangDoiCongKetNoi mockPort = (date, facilityId, departmentId) -> syntheticQueue;
        PhanQuyenDichVu mockAuth = createMockAuthzService();
        RecordingAuditPort auditPort = new RecordingAuditPort();

        GetBacSiHangDoiHandler queueHandler = new GetBacSiHangDoiHandler(mockPort, mockAuth, auditPort);
        ITienSuBenhRepository historyRepo = createMockHistoryRepo();
        GetBenhNhanTienSuHandler historyHandler = new GetBenhNhanTienSuHandler(historyRepo, mockAuth, auditPort);

        BacSiGhepNoiGoc.Services services = new BacSiGhepNoiGoc.Services(queueHandler, historyHandler);
        BacSiTramManHinh panel = new BacSiTramManHinh(services);

        runOnEdtAndWait(panel::refreshData);
        // Wait briefly for SwingWorker background execution and EDT update
        Thread.sleep(200);

        List<BenhNhanKhamDuLieu> loadedPatients = panel.getQueueSidebar().getPatients();
        assertEquals(2, loadedPatients.size());
        assertEquals("Nguyễn Văn Thực", loadedPatients.get(0).getName());
        assertEquals("BN-2026-001", loadedPatients.get(0).getCode());
        assertEquals("Trần Thị Thật", loadedPatients.get(1).getName());
        assertEquals("BN-2026-002", loadedPatients.get(1).getCode());

        // Verify the UI labels are updated for the selected patient
        List<JLabel> labels = descendantsOfType(panel, JLabel.class);
        boolean foundRealPatient = false;
        for (JLabel lbl : labels) {
            String text = lbl.getText();
            if (text != null && text.contains("Nguyễn Văn Thực")) {
                foundRealPatient = true;
            }
            if (text != null) {
                assertFalse(text.contains("TEST BN 060"));
                assertFalse(text.contains("TEST BN 033"));
            }
        }
        assertTrue(foundRealPatient, "Doctor station UI must display real patient name");
    }

    @Test
    @DisplayName("Handles empty queue gracefully without crashing or showing dummy data")
    void handlesEmptyQueueGracefully() throws Exception {
        BacSiHangDoiCongKetNoi emptyPort = (date, facilityId, departmentId) -> List.of();
        PhanQuyenDichVu mockAuth = createMockAuthzService();
        RecordingAuditPort auditPort = new RecordingAuditPort();

        GetBacSiHangDoiHandler queueHandler = new GetBacSiHangDoiHandler(emptyPort, mockAuth, auditPort);
        ITienSuBenhRepository historyRepo = createMockHistoryRepo();
        GetBenhNhanTienSuHandler historyHandler = new GetBenhNhanTienSuHandler(historyRepo, mockAuth, auditPort);

        BacSiGhepNoiGoc.Services services = new BacSiGhepNoiGoc.Services(queueHandler, historyHandler);
        BacSiTramManHinh panel = new BacSiTramManHinh(services);

        runOnEdtAndWait(panel::refreshData);
        Thread.sleep(150);

        assertEquals(0, panel.getQueueSidebar().getPatients().size());
        assertEquals(0, panel.getInFlightWorkersCount());
    }

    @Test
    @DisplayName("DoctorActionBar buttons switch clinical tabs and trigger actions")
    void actionBarSwitchesTabsCorrectly() throws Exception {
        BacSiTramManHinh panel = new BacSiTramManHinh();
        assertNotNull(panel);
        vn.clinicmanagement.desktop.modules.doctor.components.BacSiHanhDongThanh bar = panel.getActionBar();
        assertNotNull(bar);
        JTabbedPane tabs = panel.getClinicalTabs();
        assertNotNull(tabs);

        // Click Bệnh Án -> index 0
        runOnEdtAndWait(() -> bar.getBtnMedicalRecord().doClick());
        assertEquals(0, tabs.getSelectedIndex());

        // Click Dịch Vụ -> index 1
        runOnEdtAndWait(() -> bar.getBtnService().doClick());
        assertEquals(1, tabs.getSelectedIndex());

        // Click Thuốc/VT -> index 2
        runOnEdtAndWait(() -> bar.getBtnMedicine().doClick());
        assertEquals(2, tabs.getSelectedIndex());

        // Click Phác Đồ ĐT -> index 3
        runOnEdtAndWait(() -> bar.getBtnRegimen().doClick());
        assertEquals(3, tabs.getSelectedIndex());

        // Click Thanh Toán -> index 4
        runOnEdtAndWait(() -> bar.getBtnPayment().doClick());
        assertEquals(4, tabs.getSelectedIndex());

        // Click In, Làm mới, Menu (should not throw exceptions in headless mode)
        runOnEdtAndWait(() -> {
            bar.getBtnPrint().doClick();
            bar.getBtnRefresh().doClick();
            bar.getBtnMenu().doClick();
        });
    }

    private static void runOnEdtAndWait(Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private static PhanQuyenDichVu createMockAuthzService() {
        vn.clinicmanagement.domain.identity.NhanVien employee = new vn.clinicmanagement.domain.identity.NhanVien();
        employee.setId(1);
        vn.clinicmanagement.domain.identity.VaiTro role = new vn.clinicmanagement.domain.identity.VaiTro();
        role.setId(1);
        role.setTenVaiTro("DOCTOR");
        vn.clinicmanagement.domain.repositories.INhanVienRepository employees = (vn.clinicmanagement.domain.repositories.INhanVienRepository) java.lang.reflect.Proxy.newProxyInstance(
                vn.clinicmanagement.domain.repositories.INhanVienRepository.class.getClassLoader(),
                new Class<?>[]{vn.clinicmanagement.domain.repositories.INhanVienRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> Optional.of(employee);
                    case "getAssignedRoles" -> List.of(role);
                    case "getAssignedWorkScopes" -> List.of(new vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi(
                            1, "Synthetic facility", 1, "Synthetic department"));
                    default -> null;
                });
        vn.clinicmanagement.domain.system.MaTranPhanQuyen perm = new vn.clinicmanagement.domain.system.MaTranPhanQuyen();
        perm.setMaVaiTro(1);
        perm.setPhanHe("RECEPTION");
        perm.setHanhDong(vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM);
        perm.setDuocPhep(true);
        perm.setPhamViDuLieu(vn.clinicmanagement.domain.common.enums.PhamViDuLieu.KHOA_PHONG);
        vn.clinicmanagement.domain.repositories.IHeThongRepository system = new vn.clinicmanagement.domain.repositories.IHeThongRepository() {
            @Override public List<vn.clinicmanagement.domain.system.MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return List.of(perm); }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };
        return new PhanQuyenDichVu(
                sessionId -> Optional.of(new vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi.ActiveSession(
                        sessionId, 1, 1, "DOCTOR", 1, 1, true, true, true)),
                employees, system);
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container) {
                matches.addAll(descendantsOfType((Container) component, type));
            }
        }
        return matches;
    }

    private static ITienSuBenhRepository createMockHistoryRepo() {
        return (ITienSuBenhRepository) java.lang.reflect.Proxy.newProxyInstance(
                ITienSuBenhRepository.class.getClassLoader(),
                new Class<?>[]{ITienSuBenhRepository.class},
                (proxy, method, args) -> {
                    if ("getHistoryByPatientAndScope".equals(method.getName())) return List.of();
                    return null;
                });
    }

    static class RecordingAuditPort implements KiemToanWriteCongKetNoi {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    }
}

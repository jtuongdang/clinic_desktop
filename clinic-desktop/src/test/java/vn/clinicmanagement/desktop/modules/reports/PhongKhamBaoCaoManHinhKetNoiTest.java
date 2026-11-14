package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reports.GetKhoBaoCaoHandler;
import vn.clinicmanagement.application.reports.GetDoanhThuBaoCaoHandler;
import vn.clinicmanagement.application.reports.GetLuotKhamThongKeHandler;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClinicReportPanel Wiring Tests")
class PhongKhamBaoCaoManHinhKetNoiTest {

    @Test
    @DisplayName("ReportCompositionRoot builds services cleanly")
    void reportCompositionRootAssemblesCleanly() {
        BaoCaoGhepNoiGoc.Services services = BaoCaoGhepNoiGoc.createDefault();
        assertNotNull(services);
        assertNotNull(services.getVisitStatisticsHandler());
        assertNotNull(services.getRevenueReportHandler());
        assertNotNull(services.getInventoryReportHandler());
    }

    @Test
    @DisplayName("Zero occurrences of static hardcoded string 1,240 in ClinicReportPanel")
    void verifyZeroHardcodedSampleData() {
        PhongKhamBaoCaoManHinh panel = new PhongKhamBaoCaoManHinh();
        assertEquals(0, panel.getTableModel().getRowCount());

        for (int r = 0; r < panel.getTableModel().getRowCount(); r++) {
            for (int c = 0; c < panel.getTableModel().getColumnCount(); c++) {
                Object val = panel.getTableModel().getValueAt(r, c);
                if (val != null) {
                    assertFalse(val.toString().contains("1,240"), "Should not contain hardcoded sample data 1,240");
                }
            }
        }
    }

    @Test
    @DisplayName("ClinicReportPanel loads visit statistics dynamically into table")
    void panelLoadsVisitStatisticsDynamically() throws Exception {
        LuotKham lk = new LuotKham();
        lk.setId(1);
        lk.setMaBenhNhan(101);
        lk.setMaCoSo(1);
        lk.setMaKhoa(1);
        lk.setNgayKham(LocalDate.of(2026, 10, 15));
        lk.setThoiGianTiepNhan(LocalDateTime.of(2026, 10, 15, 9, 0));
        lk.setTrangThai(TrangThaiLuotKham.HOAN_THANH);

        GetLuotKhamThongKeHandler visitHandler = new GetLuotKhamThongKeHandler((f, t, fa, d) -> List.of(lk));
        GetDoanhThuBaoCaoHandler revenueHandler = new GetDoanhThuBaoCaoHandler((f, t, fa, d) -> List.of());
        GetKhoBaoCaoHandler inventoryHandler = new GetKhoBaoCaoHandler(new vn.clinicmanagement.application.reports.IKhoBaoCaoDuLieuNguon() {
            @Override public List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) { return List.of(); }
            @Override public vn.clinicmanagement.domain.inventory.MatHang getItemById(int itemId) { return null; }
            @Override public List<vn.clinicmanagement.domain.inventory.MatHang> getAllItems() { return List.of(); }
        });

        BaoCaoGhepNoiGoc.Services services = BaoCaoGhepNoiGoc.create(visitHandler, revenueHandler, inventoryHandler);

        PhongKhamBaoCaoManHinh[] panelHolder = new PhongKhamBaoCaoManHinh[1];
        SwingUtilities.invokeAndWait(() -> {
            panelHolder[0] = new PhongKhamBaoCaoManHinh(services);
            panelHolder[0].getCboReportType().setSelectedIndex(0);
            panelHolder[0].getTxtFromDate().setText("01/10/2026");
            panelHolder[0].getTxtToDate().setText("30/10/2026");
        });

        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            panelHolder[0].executeReportQuery();
            Timer timer = new Timer(300, e -> latch.countDown());
            timer.setRepeats(false);
            timer.start();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(panelHolder[0].getTableModel().getRowCount() >= 1);
        assertEquals("1", panelHolder[0].getTableModel().getValueAt(0, 0));
        assertEquals("Lượt", panelHolder[0].getTableModel().getValueAt(0, 2));
    }

    @Test
    @DisplayName("ClinicReportPanel loads revenue statistics dynamically into table")
    void panelLoadsRevenueStatisticsDynamically() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setId(1);
        hd.setMaLuotKham(1);
        hd.setMaCoSo(1);
        hd.setTongTien(new BigDecimal("5000000"));
        hd.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        hd.setSoTienBenhNhanChiTra(new BigDecimal("5000000"));
        hd.setDaThanhToan(new BigDecimal("5000000"));
        hd.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        hd.setNgayLap(LocalDateTime.of(2026, 10, 15, 10, 0));

        GetLuotKhamThongKeHandler visitHandler = new GetLuotKhamThongKeHandler((f, t, fa, d) -> List.of());
        GetDoanhThuBaoCaoHandler revenueHandler = new GetDoanhThuBaoCaoHandler((f, t, fa, d) -> List.of(hd));
        GetKhoBaoCaoHandler inventoryHandler = new GetKhoBaoCaoHandler(new vn.clinicmanagement.application.reports.IKhoBaoCaoDuLieuNguon() {
            @Override public List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) { return List.of(); }
            @Override public vn.clinicmanagement.domain.inventory.MatHang getItemById(int itemId) { return null; }
            @Override public List<vn.clinicmanagement.domain.inventory.MatHang> getAllItems() { return List.of(); }
        });

        BaoCaoGhepNoiGoc.Services services = BaoCaoGhepNoiGoc.create(visitHandler, revenueHandler, inventoryHandler);

        PhongKhamBaoCaoManHinh[] panelHolder = new PhongKhamBaoCaoManHinh[1];
        SwingUtilities.invokeAndWait(() -> {
            panelHolder[0] = new PhongKhamBaoCaoManHinh(services);
            panelHolder[0].getCboReportType().setSelectedIndex(1);
            panelHolder[0].getTxtFromDate().setText("01/10/2026");
            panelHolder[0].getTxtToDate().setText("30/10/2026");
        });

        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            panelHolder[0].executeReportQuery();
            Timer timer = new Timer(300, e -> latch.countDown());
            timer.setRepeats(false);
            timer.start();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(panelHolder[0].getTableModel().getRowCount() >= 1);
        assertEquals("1", panelHolder[0].getTableModel().getValueAt(0, 0));
        assertEquals("VNĐ", panelHolder[0].getTableModel().getValueAt(0, 2));
    }

    @Test
    @DisplayName("Export report to CSV writes correct headers and table rows")
    void exportReportToCsvWritesCorrectData() throws Exception {
        PhongKhamBaoCaoManHinh panel = new PhongKhamBaoCaoManHinh();
        panel.getTableModel().setRowCount(0);
        panel.getTableModel().addRow(new Object[]{"1", "Tổng số ca khám", "Lượt", "10", "15", "+50.0%", "Bình thường"});

        java.io.File tempFile = java.io.File.createTempFile("clinic_report_test_", ".csv");
        tempFile.deleteOnExit();

        panel.exportReportToCsv(tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);

        String content = java.nio.file.Files.readString(tempFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("STT,Chỉ Tiêu / Hạng Mục,Đơn Vị Tính"));
        assertTrue(content.contains("1,Tổng số ca khám,Lượt,10,15,+50.0%,Bình thường"));
    }

    @Test
    @DisplayName("Export and Print buttons invoke safely in headless mode")
    void exportAndPrintButtonsInvokeSafely() {
        PhongKhamBaoCaoManHinh panel = new PhongKhamBaoCaoManHinh();
        assertNotNull(panel.getBtnExport());
        assertNotNull(panel.getBtnPrint());

        assertDoesNotThrow(() -> panel.getBtnExport().doClick());
        assertDoesNotThrow(() -> panel.getBtnPrint().doClick());
        assertDoesNotThrow(panel::exportReport);
        assertDoesNotThrow(panel::printReport);
    }
}

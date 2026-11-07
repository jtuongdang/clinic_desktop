package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reports.GetInventoryReportHandler;
import vn.clinicmanagement.application.reports.GetRevenueReportHandler;
import vn.clinicmanagement.application.reports.GetVisitStatisticsHandler;
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
class ClinicReportPanelWiringTest {

    @Test
    @DisplayName("ReportCompositionRoot builds services cleanly")
    void reportCompositionRootAssemblesCleanly() {
        ReportCompositionRoot.Services services = ReportCompositionRoot.createDefault();
        assertNotNull(services);
        assertNotNull(services.getVisitStatisticsHandler());
        assertNotNull(services.getRevenueReportHandler());
        assertNotNull(services.getInventoryReportHandler());
    }

    @Test
    @DisplayName("Zero occurrences of static hardcoded string 1,240 in ClinicReportPanel")
    void verifyZeroHardcodedSampleData() {
        ClinicReportPanel panel = new ClinicReportPanel();
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

        GetVisitStatisticsHandler visitHandler = new GetVisitStatisticsHandler((f, t, fa, d) -> List.of(lk));
        GetRevenueReportHandler revenueHandler = new GetRevenueReportHandler((f, t, fa, d) -> List.of());
        GetInventoryReportHandler inventoryHandler = new GetInventoryReportHandler(new vn.clinicmanagement.application.reports.IInventoryReportDataSource() {
            @Override public List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) { return List.of(); }
            @Override public vn.clinicmanagement.domain.inventory.Item getItemById(int itemId) { return null; }
            @Override public List<vn.clinicmanagement.domain.inventory.Item> getAllItems() { return List.of(); }
        });

        ReportCompositionRoot.Services services = ReportCompositionRoot.create(visitHandler, revenueHandler, inventoryHandler);

        ClinicReportPanel[] panelHolder = new ClinicReportPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panelHolder[0] = new ClinicReportPanel(services);
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

        GetVisitStatisticsHandler visitHandler = new GetVisitStatisticsHandler((f, t, fa, d) -> List.of());
        GetRevenueReportHandler revenueHandler = new GetRevenueReportHandler((f, t, fa, d) -> List.of(hd));
        GetInventoryReportHandler inventoryHandler = new GetInventoryReportHandler(new vn.clinicmanagement.application.reports.IInventoryReportDataSource() {
            @Override public List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) { return List.of(); }
            @Override public vn.clinicmanagement.domain.inventory.Item getItemById(int itemId) { return null; }
            @Override public List<vn.clinicmanagement.domain.inventory.Item> getAllItems() { return List.of(); }
        });

        ReportCompositionRoot.Services services = ReportCompositionRoot.create(visitHandler, revenueHandler, inventoryHandler);

        ClinicReportPanel[] panelHolder = new ClinicReportPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panelHolder[0] = new ClinicReportPanel(services);
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
}

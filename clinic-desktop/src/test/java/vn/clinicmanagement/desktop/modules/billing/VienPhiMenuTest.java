package vn.clinicmanagement.desktop.modules.billing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort.*;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.billing.VienPhiMenu.MucMenu;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VienPhiMenuTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void menuMatchesReferenceStructureAndDispatchesAllItems() {
        var selected = new AtomicReference<MucMenu>();
        var menu = new VienPhiMenu(selected::set);

        // 2 top items + "Cấu Hình" + 1 item + "Quản Lý" + 6 items + 1 footer item = 12 components
        assertEquals(12, menu.getComponentCount());

        assertEquals("Quản Lý Viện Phí", ((JMenuItem) menu.getComponent(0)).getText());
        assertEquals("Chốt Số Liệu Phiếu Thu", ((JMenuItem) menu.getComponent(1)).getText());
        assertEquals("Cấu Hình", ((JLabel) menu.getComponent(2)).getText());
        assertEquals("Tùy Chọn", ((JMenuItem) menu.getComponent(3)).getText());
        assertEquals("Quản Lý", ((JLabel) menu.getComponent(4)).getText());
        assertEquals("Danh Sách Phiếu Thu", ((JMenuItem) menu.getComponent(5)).getText());
        assertEquals("Danh Sách Sổ Thu Tiền", ((JMenuItem) menu.getComponent(6)).getText());
        assertEquals("Chọn Sổ Thu Tiền", ((JMenuItem) menu.getComponent(7)).getText());
        assertEquals("Lọc Khoa Phòng", ((JMenuItem) menu.getComponent(8)).getText());
        assertEquals("Màn Hình Chờ", ((JMenuItem) menu.getComponent(9)).getText());
        assertEquals("Cấu Hình Phát Loa", ((JMenuItem) menu.getComponent(10)).getText());
        assertEquals("Hóa Đơn Điện Tử", ((JMenuItem) menu.getComponent(11)).getText());

        int[] itemIndices = {0, 1, 3, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < itemIndices.length; i++) {
            ((JMenuItem) menu.getComponent(itemIndices[i])).doClick();
            assertEquals(MucMenu.values()[i], selected.get());
        }
    }

    @Test
    void receiptListPanelQueriesDatabasePort() throws Exception {
        PhienThuNganKiemThu.login("CASHIER");
        QuanLyDoanhThuReadPort fakePort = new QuanLyDoanhThuReadPort() {
            @Override public List<RevenueVisitRow> listVisits(int f, boolean d, LocalDateTime from, LocalDateTime to) { return List.of(); }
            @Override public List<ReceiptReportRow> listReceiptReport(int facilityId, boolean filterByDate, int timeModeIndex, LocalDateTime from, LocalDateTime to, int sortModeIndex) {
                assertEquals(1, facilityId);
                return List.of(new ReceiptReportRow(
                        1, 10, "PT-0001", LocalDateTime.of(2023, 8, 13, 10, 30), LocalDateTime.of(2023, 8, 13, 9, 0),
                        "Nguyễn Văn A", LocalDate.of(1990, 1, 1), "Nam", "TP.HCM",
                        "Phạm Quang Dương", "TIEN_MAT", "BHYT", "NGOAI_TRU", "KHÁM",
                        "Khoa Khám", "Phòng 1", 1, 0, 0, 1,
                        new BigDecimal("250000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"),
                        "", "", "Khám bệnh", "duongpq test", false, "THU_TIEN", true, "DA_THANH_TOAN"
                ));
            }
            @Override public List<String> listReceiptFilterValues(int f, String k) { return List.of(); }
            @Override public List<ServiceTypeReportRow> listServiceTypeReport(int f, LocalDateTime from, LocalDateTime to, Integer r, String g, String s, boolean gd, boolean gr) { return List.of(); }
            @Override public List<String> listServiceNames() { return List.of(); }
            @Override public List<RoomOption> listClinicRooms(int f) { return List.of(); }
            @Override public List<RoomVisitDetail> listRoomReport(int f, LocalDateTime from, LocalDateTime to, Integer r, boolean gd, boolean ws, boolean po) { return List.of(); }
            @Override public List<String> listService019FilterValues(int f, String k) { return List.of(); }
            @Override public List<Service019RawRow> listService019Report(int f, int s, LocalDateTime from, LocalDateTime to) { return List.of(); }
            @Override public ClinicSummaryStats getClinicSummary(int f, LocalDateTime from, LocalDateTime to) {
                return new ClinicSummaryStats(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }
        };

        VienPhiHoaDonManHinh[] screen = new VienPhiHoaDonManHinh[1];
        DanhSachPhieuThuHopThoai[] receiptPanel = new DanhSachPhieuThuHopThoai[1];
        SwingUtilities.invokeAndWait(() -> {
            screen[0] = new VienPhiHoaDonManHinh();
            screen[0].setReceiptReadPort(fakePort);
            receiptPanel[0] = screen[0].createReceiptListPanel();
        });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            int[] rows = {0};
            SwingUtilities.invokeAndWait(() -> rows[0] = receiptPanel[0].getTable().getRowCount());
            if (rows[0] == 1) break;
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, receiptPanel[0].getTable().getRowCount());
            assertEquals("PT-0001", receiptPanel[0].getTable().getValueAt(0, 1));
            assertEquals("Nguyễn Văn A", receiptPanel[0].getTable().getValueAt(0, 3));
            assertTrue(receiptPanel[0].getLblTotals().getText().contains("250,000"));
        });
    }
}

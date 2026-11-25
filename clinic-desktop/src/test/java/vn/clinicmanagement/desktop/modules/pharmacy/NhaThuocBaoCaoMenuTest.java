package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.BaoCaoKetQua;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonOptions;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintData;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintRow;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBaoCaoHopThoai;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBaoCaoMenu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.TuyChonBaoCaoXuatNhapTonHopThoai;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NhaThuocBaoCaoMenuTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void menuMatchesReferenceAndDispatchesAllThirteenReports() {
        var selected = new AtomicReference<LoaiBaoCao>();
        var menu = new NhaThuocBaoCaoMenu(selected::set);

        // 2 group headers ("Phiếu", "Báo Cáo") + 13 report items = 15 components
        assertEquals(15, menu.getComponentCount());
        assertEquals("Phiếu", ((JLabel) menu.getComponent(0)).getText());
        assertEquals("Báo Cáo", ((JLabel) menu.getComponent(1)).getText());

        String[] expectedTitles = {
                "Báo Cáo Xuất Nhập Tồn",
                "Báo Cáo Doanh Thu Nhà Thuốc",
                "Báo Cáo Lợi Nhuận Nhà Thuốc",
                "Phiếu Hoàn Trả Thuốc",
                "Báo Cáo Nhập Trả Lẻ",
                "Báo Cáo Phiếu Nhập Nhà Cung Cấp",
                "Báo Cáo Thuốc Nhập Nhà Cung Cấp",
                "Báo Cáo Kiểm Nhập Thuốc Vật Tư (Ncc)",
                "Biên Bản Kiểm Nhập Thuốc Từ Nhà Cung Cấp",
                "Báo Cáo Xuất Thuốc Theo Phiếu",
                "Báo Cáo Mua Hàng Nhập Kho",
                "Báo Cáo Xuất Bán Hàng",
                "Báo Cáo Thu Hồi Khả Dụng"
        };

        for (int i = 0; i < expectedTitles.length; i++) {
            var item = (JMenuItem) menu.getComponent(i + 2);
            assertEquals(expectedTitles[i], item.getText());
            item.doClick();
            assertEquals(LoaiBaoCao.values()[i], selected.get());
        }
    }

    @Test
    void pharmacyScreenLoadsReportViaPort() throws Exception {
        PhienThuNganKiemThu.login("PHARMACIST");
        NhaThuocBaoCaoReadPort fakePort = (type, facilityId, warehouseId, from, to) -> {
            assertEquals(1, facilityId);
            return new BaoCaoKetQua(
                    type.title(),
                    List.of("Mã thuốc", "Tên thuốc", "Tổng tiền"),
                    List.of(List.of("TH01", "Paracetamol", new BigDecimal("125000"))),
                    "Số dòng: 1   |   Tổng: 125,000"
            );
        };

        NhaThuocDispenseManHinh[] screen = new NhaThuocDispenseManHinh[1];
        SwingUtilities.invokeAndWait(() ->
                screen[0] = new NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.createDefault(), null, fakePort));

        NhaThuocBaoCaoHopThoai[] dialogPanel = new NhaThuocBaoCaoHopThoai[1];
        SwingUtilities.invokeAndWait(() ->
                dialogPanel[0] = screen[0].createReportPanel(LoaiBaoCao.XUAT_NHAP_TON));

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            int[] rows = {0};
            SwingUtilities.invokeAndWait(() -> rows[0] = dialogPanel[0].getTable().getRowCount());
            if (rows[0] == 1) {
                break;
            }
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, dialogPanel[0].getTable().getRowCount());
            assertEquals("TH01", dialogPanel[0].getTable().getValueAt(0, 0));
            assertEquals("Số dòng: 1   |   Tổng: 125,000", dialogPanel[0].getLblSummary().getText());
        });
    }

    @Test
    void xuatNhapTonOptionsDialogDispatchesSelectedOptionsToPort() throws Exception {
        PhienThuNganKiemThu.login("PHARMACIST");
        var capturedOptions = new AtomicReference<XuatNhapTonOptions>();
        NhaThuocBaoCaoReadPort fakePort = new NhaThuocBaoCaoReadPort() {
            @Override
            public BaoCaoKetQua getReport(LoaiBaoCao type, int facilityId, Integer warehouseId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
                return new BaoCaoKetQua(type.title(), List.of("A"), List.of(), "");
            }

            @Override
            public XuatNhapTonPrintData getXuatNhapTonPrintData(int facilityId, Integer warehouseId, java.time.LocalDateTime from, java.time.LocalDateTime to, XuatNhapTonOptions options) {
                capturedOptions.set(options);
                return new XuatNhapTonPrintData(
                        "Nhà thuốc test",
                        List.of(new XuatNhapTonPrintRow(
                                "(Không nhóm dữ liệu)", "TH02", "Thuốc test", "50mg", "", "Viên", "01-09-2023",
                                new BigDecimal("5000"), new BigDecimal("5000"),
                                BigDecimal.ZERO, BigDecimal.ZERO,
                                new BigDecimal("80"), new BigDecimal("400000"),
                                BigDecimal.ZERO, BigDecimal.ZERO,
                                new BigDecimal("80"), new BigDecimal("400000")
                        ))
                );
            }
        };

        NhaThuocDispenseManHinh[] screen = new NhaThuocDispenseManHinh[1];
        SwingUtilities.invokeAndWait(() ->
                screen[0] = new NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.createDefault(), null, fakePort));

        var loadedPrintData = new AtomicReference<XuatNhapTonPrintData>();
        SwingUtilities.invokeAndWait(() -> {
            TuyChonBaoCaoXuatNhapTonHopThoai optsPanel = screen[0].createXuatNhapTonOptionsPanel((from, to, options) ->
                    loadedPrintData.set(fakePort.getXuatNhapTonPrintData(1, null, from, to, options)));
            optsPanel.getRadWarehouse().setSelected(true);
            optsPanel.getChkIncludeLocked().setSelected(true);
            optsPanel.getCboPrintMode().setSelectedIndex(2); // STOCK_GT_ZERO
            optsPanel.getBtnOk().doClick();
        });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            if (capturedOptions.get() != null) break;
        }
        assertNotNull(capturedOptions.get());
        assertNotNull(loadedPrintData.get());
        assertEquals("Nhà thuốc test", loadedPrintData.get().warehouseName());
        assertEquals(1, loadedPrintData.get().rows().size());
        assertEquals("WAREHOUSE", capturedOptions.get().groupBy());
        assertTrue(capturedOptions.get().includeLockedItems());
        assertEquals("STOCK_GT_ZERO", capturedOptions.get().printFilter());
    }
}

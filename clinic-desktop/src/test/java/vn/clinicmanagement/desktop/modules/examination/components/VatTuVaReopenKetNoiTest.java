package vn.clinicmanagement.desktop.modules.examination.components;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauTheManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.VatTuYeuCauTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenYTeRecordManHinh.ReopenRecordRequest;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenYTeRecordManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauManHinh;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.prescription.DonThuocDuLieu;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDuLieu;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Tests: Supplies & Reopen Record Wiring")
class VatTuVaReopenKetNoiTest {

    @BeforeAll
    static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private AdminPatientItem createTestPatient() {
        return new AdminPatientItem(
                "1",
                "NGUYỄN VĂN AN",
                "22001122",
                "Viện phí",
                "28 tuổi",
                false,
                "Nam",
                "1995",
                "28 tuổi",
                "28 tuổi",
                "15/05/1995",
                "123 Hoàng Quốc Việt, Cầu Giấy, Hà Nội",
                "Cầu Giấy, Hà Nội",
                "0987654321",
                "BA-9999",
                "08:30 15/10/2023",
                "PK Nội Tổng Hợp",
                "BS. Nguyễn Văn A",
                "K29-Viêm dạ dày tá tràng",
                "VP-105"
        );
    }

    @Test
    @DisplayName("VatTuYeuCauTheManHinh: Khởi tạo và nạp phiếu vật tư thật từ DonThuocDuLieu")
    void testSuppliesOrderTabPanelPatientAndRealDataBinding() {
        AdminPatientItem patient = createTestPatient();
        VatTuYeuCauTheManHinh panel = new VatTuYeuCauTheManHinh(patient);
        panel.setSuppressDialogs(true);

        AtomicInteger supplyCount = new AtomicInteger(-1);
        panel.setOnSupplyCountChanged(supplyCount::set);

        assertNotNull(panel.getTableModel());
        assertNotNull(panel.getSuppliesTable());
        assertEquals("K29-Viêm dạ dày tá tràng", panel.getLblChanDoan().getText());
        assertEquals(patient, panel.getPatient());

        // Phiếu thuốc thông thường không được hiển thị ở tab Vật Tư
        DonThuocDuLieu rxDrugOnly = new DonThuocDuLieu(
                90, 105, 2, "BS. Nguyễn Văn A", 1, 1,
                TrangThaiDonThuoc.TAM, LocalDateTime.of(2026, 11, 18, 15, 5),
                "Kê đơn từ bàn khám", "", new BigDecimal("7500"),
                List.of(new DonThuocMatHangDuLieu(
                        1, 1, "THUOC001", "Lactulose 10g", "Gói",
                        BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("7500"), new BigDecimal("7500"),
                        "Uống", "1 gói", "", 1, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "",
                        LoaiMatHang.THUOC)));

        // Phiếu vật tư y tế phải hiển thị ở tab Vật Tư
        DonThuocDuLieu rxSupply = new DonThuocDuLieu(
                91, 105, 2, "BS. Nguyễn Văn A", 1, 4,
                TrangThaiDonThuoc.TAM, LocalDateTime.of(2026, 11, 18, 15, 10),
                "[VAT_TU] Chỉ định vật tư y tế từ bàn khám", "", new BigDecimal("1470"),
                List.of(new DonThuocMatHangDuLieu(
                        2, 16, "VT001", "Bơm tiêm sử dụng một lần 5ml (Vinahankook)", "Cái",
                        new BigDecimal("2"), BigDecimal.ZERO, new BigDecimal("735"), new BigDecimal("1470"),
                        "Vật tư y tế", "Số lượng 2 Cái", "", 1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        "Xuất từ Kho Vật Tư Y Tế", LoaiMatHang.VAT_TU)));

        panel.applyLoadedSupplyOrders(List.of(rxDrugOnly, rxSupply));

        assertEquals(1, supplyCount.get());
        assertEquals("VT-91", panel.getLblMaPhieu().getText());
        assertEquals("VT001", panel.getTableModel().getValueAt(0, 1));
        assertTrue(panel.getTableModel().getValueAt(0, 2).toString().contains("Bơm tiêm sử dụng một lần 5ml"));
        assertEquals("2", panel.getTableModel().getValueAt(0, 4));
        assertDoesNotThrow(panel::executePrint);

        // Ngược lại, DonThuocYeuCauTheManHinh phải lọc bỏ phiếu VAT_TU
        DonThuocYeuCauTheManHinh drugTab = new DonThuocYeuCauTheManHinh(KhamBenhGhepNoiGoc.createDefault());
        AtomicInteger drugCount = new AtomicInteger(-1);
        drugTab.setOnPrescriptionCountChanged(drugCount::set);
        drugTab.applyLoadedPrescriptions(List.of(rxDrugOnly, rxSupply));
        assertEquals(1, drugCount.get());
    }

    @Test
    @DisplayName("VatTuYeuCauTheManHinh: Hủy phiếu cập nhật trạng thái và dòng bảng")
    void testSuppliesOrderTabPanelCancelFlow() {
        AdminPatientItem patient = createTestPatient();
        VatTuYeuCauTheManHinh panel = new VatTuYeuCauTheManHinh(patient);
        panel.setSuppressDialogs(true);

        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{"1", "VT001", "Bơm tiêm sử dụng một lần 5ml", "Cái", "2"});
        panel.setSupplies(rows);

        assertFalse(panel.isCancelled());
        panel.executeCancel();

        assertTrue(panel.isCancelled());
        assertTrue(panel.getLblTrangThai().getText().contains("ĐÃ HỦY PHIẾU"));

        DefaultTableModel model = panel.getTableModel();
        String itemCol = (String) model.getValueAt(0, 2);
        assertTrue(itemCol.contains("[Đã hủy]"), "Dòng vật tư phải có nhãn [Đã hủy]");
    }

    @Test
    @DisplayName("YTeVatTuYeuCauManHinh: Nạp danh mục VAT_TU từ IKhoRepository, thêm nháp và tạo PrescribeThuocCommand")
    void testMedicalSuppliesOrderPanelCatalogDraftAndCommand() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        MatHang syringe = new MatHang();
        syringe.setId(16);
        syringe.setMaItem("VT001");
        syringe.setTenItem("Bơm tiêm sử dụng một lần 5ml (Vinahankook)");
        syringe.setLoaiItem(LoaiMatHang.VAT_TU);
        syringe.setHoatChat("Vật tư tiêu hao");
        syringe.setDonViCoBan("Cái");
        syringe.setGiaBanMacDinh(new BigDecimal("735"));
        syringe.setHoatDong(true);
        TonKho tk = new TonKho();
        tk.setId(10);
        tk.setMaKho(4);
        tk.setMaItem(16);
        tk.setSoLuongTon(new BigDecimal("1000"));
        tk.setHanSuDung(LocalDate.of(2028, 12, 31));
        syringe.setTonKhos(List.of(tk));
        services.inventoryRepository().saveItem(syringe);

        // Thuốc không được hiển thị trong danh mục Vật Tư
        MatHang drug = new MatHang();
        drug.setId(1);
        drug.setMaItem("THUOC001");
        drug.setTenItem("Paracetamol 500mg");
        drug.setLoaiItem(LoaiMatHang.THUOC);
        drug.setDonViCoBan("Viên");
        drug.setGiaBanMacDinh(new BigDecimal("1000"));
        drug.setHoatDong(true);
        services.inventoryRepository().saveItem(drug);

        AdminPatientItem patient = createTestPatient();
        YTeVatTuYeuCauManHinh orderPanel = new YTeVatTuYeuCauManHinh(() -> {}, services);
        orderPanel.setSuppressDialogs(true);
        orderPanel.setVisitContext(105, 2, 1);
        orderPanel.setPatient(patient);

        assertEquals(1, orderPanel.getAllSupplies().size());
        assertEquals("VT001", orderPanel.getAllSupplies().get(0).code());
        assertFalse(orderPanel.getTxtStore().getText().equals("Cái"), "Ô Tên kho không được hiển thị nhầm đơn vị Cái");

        orderPanel.getTxtQty().setText("3");
        orderPanel.confirmSupplyDraft(orderPanel.getAllSupplies().get(0), new BigDecimal("3"));

        assertEquals(1, orderPanel.getDraftSupplyItems().size());
        assertEquals("2,205", orderPanel.getLblTotalCostVal().getText());

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(2, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        PrescribeThuocCommand cmd = orderPanel.buildPrescribeCommand(auth);

        assertNotNull(orderPanel.getBtnClose());
        assertEquals(" Đóng", orderPanel.getBtnClose().getText());
        assertEquals(105, cmd.visitId());
        assertEquals(4, cmd.warehouseId());
        assertTrue(cmd.ghiChu().startsWith("[VAT_TU]"));
        assertEquals(1, cmd.items().size());
        assertEquals(16, cmd.items().get(0).itemId());
        assertEquals(0, cmd.items().get(0).soLuongKe().compareTo(new BigDecimal("3")));

        AtomicBoolean closed = new AtomicBoolean(false);
        YTeVatTuYeuCauManHinh closeOrderPanel = new YTeVatTuYeuCauManHinh(() -> closed.set(true), services);
        closeOrderPanel.setSuppressDialogs(true);
        closeOrderPanel.getBtnClose().doClick();
        assertTrue(closed.get(), "Bấm Đóng phải quay lại màn hình Khám Bệnh mà không cần lưu");
    }

    @Test
    @DisplayName("Đấu nối callback nội bộ giữa YTeVatTuYeuCauManHinh và VatTuYeuCauTheManHinh")
    void testSuppliesWiringEndToEndFallback() {
        AdminPatientItem patient = createTestPatient();
        VatTuYeuCauTheManHinh tabPanel = new VatTuYeuCauTheManHinh(patient);
        tabPanel.setSuppressDialogs(true);

        YTeVatTuYeuCauManHinh orderPanel = new YTeVatTuYeuCauManHinh();
        orderPanel.setSuppressDialogs(true);
        orderPanel.setPatient(patient);
        orderPanel.loadSupplyCatalogItems(List.of(
                new YTeVatTuYeuCauManHinh.SupplyCatalogItem(
                        16, "VT001", "Bơm tiêm sử dụng một lần 5ml", "Cái", "Vật tư tiêu hao",
                        4, "Kho Vật Tư Y Tế", new BigDecimal("1000"), new BigDecimal("735"))));
        orderPanel.setOnSuppliesSavedCallback(tabPanel::setSupplies);

        orderPanel.confirmSupplyDraft(orderPanel.getAllSupplies().get(0), new BigDecimal("2"));
        orderPanel.executeSave();

        assertFalse(tabPanel.isCancelled());
        DefaultTableModel tabModel = tabPanel.getTableModel();
        assertEquals("VT001", tabModel.getValueAt(0, 1));
        assertTrue(tabModel.getValueAt(0, 2).toString().contains("Bơm tiêm sử dụng một lần 5ml"));
        assertEquals("2", tabModel.getValueAt(0, 4));
    }

    @Test
    @DisplayName("ReopenMedicalRecordPanel: Kiểm tra xác thực lý do và kích hoạt mở bệnh án")
    void testReopenMedicalRecordPanelFlow() {
        AdminPatientItem patient = createTestPatient();
        AtomicReference<ReopenRecordRequest> requestRef = new AtomicReference<>();
        AtomicBoolean closedRef = new AtomicBoolean(false);

        ReopenYTeRecordManHinh reopenPanel = new ReopenYTeRecordManHinh(
                patient,
                requestRef::set,
                () -> closedRef.set(true)
        );
        reopenPanel.setSuppressDialogs(true);

        reopenPanel.getTxtReason().setText("");
        reopenPanel.executeSave();
        assertNull(requestRef.get(), "Không được kích hoạt callback khi lý do trống");
        assertFalse(closedRef.get());

        reopenPanel.getTxtReason().setText("Bác sĩ yêu cầu bổ sung kết quả xét nghiệm");
        reopenPanel.executeSave();

        assertNotNull(requestRef.get(), "Callback phải nhận được request");
        assertEquals("22001122", requestRef.get().patientCode);
        assertEquals("NGUYỄN VĂN AN", requestRef.get().patientName);
        assertEquals("Bác sĩ yêu cầu bổ sung kết quả xét nghiệm", requestRef.get().reason);
        assertTrue(closedRef.get(), "Hộp thoại phải được đóng lại sau khi lưu thành công");
    }
}

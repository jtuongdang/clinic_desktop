package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrescriptionOrderPanel Behavior Tests")
class DonThuocYeuCauManHinhTest {

    @Test
    @DisplayName("Loads real catalog items from IKhoRepository, drafts items on OK, and builds exact PrescribeThuocCommand")
    void loadsCatalogFromRepositoryDraftsAndBuildsCommand() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        MatHang gastro = new MatHang();
        gastro.setId(42);
        gastro.setMaItem("THUOC004");
        gastro.setTenItem("Gastropulgite 2.5g");
        gastro.setLoaiItem(LoaiMatHang.THUOC);
        gastro.setDonViCoBan("Gói");
        gastro.setQuyCachDongGoi("Hộp 30 gói");
        gastro.setGiaBanMacDinh(new BigDecimal("3500"));
        gastro.setDangBaoChe("Uống");
        gastro.setHoatDong(true);
        TonKho tk = new TonKho();
        tk.setId(1);
        tk.setMaKho(1);
        tk.setMaItem(42);
        tk.setSoLuongTon(new BigDecimal("1000"));
        tk.setHanSuDung(LocalDate.of(2028, 12, 31));
        gastro.setTonKhos(List.of(tk));
        services.inventoryRepository().saveItem(gastro);

        DonThuocYeuCauManHinh panel = new DonThuocYeuCauManHinh(() -> {}, services);
        panel.setVisitContext(15, 2, 1);
        panel.setPatientContext(new KhamBenhManHinh.AdminPatientItem(
                "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4", true,
                "Nam", "2022", "4 tuổi", "4 tuổi", "01/01/2022",
                "TP Bắc Ninh", "TP Bắc Ninh", "0912345002", "BA-15",
                "18/11/2026", "Khoa Khám Bệnh", "BS. Lê Văn An", "Viêm họng cấp", "VP-15"));

        panel.showSelectedMedicineDemoState("THUOC004");
        panel.showPrescribedAfterOkDemoState();

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                2, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        PrescribeThuocCommand cmd = panel.buildPrescribeCommand(auth);

        assertEquals(15, cmd.visitId());
        assertEquals(2, cmd.doctorId());
        assertEquals(1, cmd.facilityId());
        assertEquals(1, cmd.items().size());
        assertEquals(42, cmd.items().get(0).itemId());
        assertEquals(0, cmd.items().get(0).soLuongKe().compareTo(new BigDecimal("10")));
        assertEquals("Uống", cmd.items().get(0).duongDung());
        assertTrue(panel.isOrderStatusBarVisible());
        assertTrue(panel.getOrderStatusText().contains("Đã kê: 1 thuốc"));

        panel.removeDraftItemAt(0);
        assertEquals(0, panel.getDraftItemCount());
        assertFalse(panel.isOrderStatusBarVisible());
        assertEquals("0", panel.getTienDonText());

        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        DonThuocYeuCauManHinh closePanel = new DonThuocYeuCauManHinh(() -> closed.set(true), services);
        closePanel.closeWithoutSaving();
        assertTrue(closed.get(), "Nút Đóng phải gọi onCloseCallback quay lại màn hình Khám Bệnh");
    }

    @Test
    @DisplayName("ICD selector loads catalog from examQueue and syncs selected code, name, and ICD id")
    void icdSelectorLoadsCatalogAndSyncsSelection() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DonThuocYeuCauManHinh panel = new DonThuocYeuCauManHinh(() -> {}, services, false);

        var selector = panel.getIcdSelector();
        assertNotNull(selector);
        assertFalse(selector.getIcdCatalog().isEmpty(), "Phải nạp danh mục ICD từ CSDL / examQueue");

        selector.applyIcd("J02", "Viêm họng cấp");
        assertNotNull(selector.getSelectedIcdId());
        assertEquals("Viêm họng cấp", selector.getDiagnosisTextOnly());
        assertEquals("J02 - Viêm họng cấp", selector.getDiagnosisDisplay());

        selector.setDiagnosis("I10 - Tăng huyết áp vô căn");
        assertNotNull(selector.getSelectedIcdId());
        assertEquals("I10 - Tăng huyết áp vô căn", selector.getDiagnosisDisplay());
    }
}

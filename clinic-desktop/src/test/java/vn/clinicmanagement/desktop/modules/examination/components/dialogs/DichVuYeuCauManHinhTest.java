package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.application.cls.request.YeuCauDichVuCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;

import javax.swing.tree.DefaultMutableTreeNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DichVuYeuCauManHinhTest {

    @Test
    void buildsDynamicCategoryTreeAndFiltersByGroupTypeAndSearch() {
        DichVuYeuCauManHinh panel = new DichVuYeuCauManHinh(() -> {}, null, false);
        panel.loadCatalogItems(sampleCatalog());

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) panel.getCategoryTree().getModel().getRoot();
        assertEquals("Dịch vụ cận lâm sàng (4)", root.getUserObject().toString());
        assertEquals(2, root.getChildCount());
        assertEquals("Xét nghiệm (3)", ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject().toString());
        assertEquals("Chẩn đoán hình ảnh (1)", ((DefaultMutableTreeNode) root.getChildAt(1)).getUserObject().toString());

        // Tự động lọc theo nhóm đầu tiên ("Hóa sinh") khi nạp xong
        assertEquals(2, panel.getCurrentFilteredServices().size());
        assertTrue(panel.getCurrentFilteredServices().stream().allMatch(s -> "Hóa sinh".equals(s.category)));

        // Lọc theo nhóm Huyết học (kèm count trên nhãn cây)
        panel.filterServicesByCategory("Huyết học (1)");
        assertEquals(1, panel.getCurrentFilteredServices().size());
        assertEquals("XN_CTM", panel.getCurrentFilteredServices().get(0).code);

        // Lọc toàn bộ Xét nghiệm
        panel.filterServicesByCategory("Xét nghiệm (3)");
        assertEquals(3, panel.getCurrentFilteredServices().size());

        // Lọc tất cả + tìm kiếm từ khóa
        panel.filterServicesByCategory("Tất cả");
        assertEquals(4, panel.getCurrentFilteredServices().size());
        panel.getSearchField().setText("Glucose");
        assertEquals(1, panel.getCurrentFilteredServices().size());
        assertEquals("XN_GLUCOSE", panel.getCurrentFilteredServices().get(0).code);
    }

    @Test
    void bindsRealPatientContextAndBuildsMultiServiceOrderCommands() {
        DichVuYeuCauManHinh panel = new DichVuYeuCauManHinh(() -> {}, null, false);
        panel.loadCatalogItems(sampleCatalog());

        KhamBenhManHinh.AdminPatientItem patient = new KhamBenhManHinh.AdminPatientItem(
                "1", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4", true,
                "Nam", "2022", "4 tuổi", "4 tuổi", "2022-10-20",
                "Phường Võ Cường, TP Bắc Ninh", "Phường Võ Cường, TP Bắc Ninh", "0912345002",
                "BA-2020", "09:10 18/11/2026", "Khoa Khám Bệnh", "BS. Tiến",
                "J06 - Viêm đường hô hấp trên cấp", "VP-2020");

        panel.setVisitContext(2020, 2, 1);
        panel.setPatientContext(patient);

        assertTrue(panel.getBannerPatientText().contains("TRẦN MINH KHÔI"));
        assertTrue(panel.getBannerPatientText().contains("BN260002"));
        assertFalse(panel.getBannerPatientText().contains("NGUYỄN HOÀNG LONG"));

        panel.filterServicesByCategory("Tất cả");
        List<DichVuYeuCauManHinh.ServiceItem> items = panel.getCurrentFilteredServices();
        panel.toggleServiceSelection(items.get(0));
        panel.toggleServiceSelection(items.get(1));

        assertEquals(2, panel.getSelectedServices().size());
        assertTrue(panel.getTotalCostLabelText().contains("85,000"));
        assertFalse(panel.getTotalCostLabelText().contains("21,611,700"));

        PhanQuyenNguCanh ctx = new PhanQuyenNguCanh(2, 1, "DOCTOR", 1, 1, java.util.UUID.randomUUID());
        List<YeuCauDichVuCommand> cmds = panel.buildOrderCommands(ctx);
        assertEquals(2, cmds.size());
        assertEquals(2020, cmds.get(0).visitId());
        assertEquals(items.get(0).serviceId, cmds.get(0).serviceId());
        assertEquals(items.get(1).serviceId, cmds.get(1).serviceId());
    }

    @Test
    void orderedServicesTabFiltersOutCanceledAndExamFeeRows() {
        DichVuYeuCauManHinh panel = new DichVuYeuCauManHinh(() -> {}, null, false);
        panel.loadCatalogItems(sampleCatalog());

        panel.applyOrderedVisitServices(List.of(
                new DichVuYeuCauDuLieu(
                        21, 2020, 4, "XN_GLUCOSE", "Định lượng Glucose máu", "XET_NGHIEM",
                        2, BigDecimal.ONE, new BigDecimal("45000"), "DA_CHI_DINH",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 20), null, null, List.of()),
                new DichVuYeuCauDuLieu(
                        22, 2020, 1, "DV_KHAM_NHI", "Khám Nhi chuyên khoa", "KHAM_BENH",
                        2, BigDecimal.ONE, new BigDecimal("120000"), "DA_CHI_DINH",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 10), null, null, List.of()),
                new DichVuYeuCauDuLieu(
                        23, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                        2, BigDecimal.ONE, new BigDecimal("85000"), "DA_HUY",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 22), null, null, List.of())
        ));

        assertEquals(1, panel.getOrderedServicesRowCount());
    }

    @Test
    void mergeOrdersForPrintDeduplicatesNewlySavedOrderAlreadyRefreshedFromDatabase() {
        var persistedOrder = new DichVuYeuCauDuLieu(
                55, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                2, BigDecimal.ONE, new BigDecimal("85000"), "DA_CHI_DINH",
                null, null, null, null, null,
                LocalDateTime.of(2026, 11, 18, 23, 38), null, null, List.of());
        var justSavedCopy = new DichVuYeuCauDuLieu(
                55, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                2, BigDecimal.ONE, new BigDecimal("85000"), "DA_CHI_DINH",
                null, null, null, null, null,
                LocalDateTime.of(2026, 11, 18, 23, 38), null, null, List.of());
        var secondOrder = new DichVuYeuCauDuLieu(
                56, 2020, 4, "XN_GLUCOSE", "Định lượng Glucose máu", "XET_NGHIEM",
                2, BigDecimal.ONE, new BigDecimal("45000"), "DA_CHI_DINH",
                null, null, null, null, null,
                LocalDateTime.of(2026, 11, 18, 23, 38), null, null, List.of());

        List<DichVuYeuCauDuLieu> merged = DichVuYeuCauManHinh.mergeOrdersForPrint(
                List.of(persistedOrder),
                List.of(justSavedCopy, secondOrder));

        assertEquals(2, merged.size());
        assertEquals(55, merged.get(0).orderId());
        assertEquals(56, merged.get(1).orderId());
    }

    private static List<DichVuYeuCauManHinh.ServiceItem> sampleCatalog() {
        return List.of(
                new DichVuYeuCauManHinh.ServiceItem(
                        4, "XET_NGHIEM", "Hóa sinh", "XN_GLUCOSE", "", "Định lượng Glucose máu",
                        0, new BigDecimal("25000"), new BigDecimal("45000"),
                        "25,000", "45,000", "45,000", "45,000", "", "Phòng Xét nghiệm", false),
                new DichVuYeuCauManHinh.ServiceItem(
                        18, "XET_NGHIEM", "Hóa sinh", "XN_HS_URE_CRE", "", "Định lượng Ure máu",
                        0, new BigDecimal("21500"), new BigDecimal("40000"),
                        "21,500", "40,000", "40,000", "40,000", "", "Phòng Xét nghiệm", false),
                new DichVuYeuCauManHinh.ServiceItem(
                        3, "XET_NGHIEM", "Huyết học", "XN_CTM", "", "Tổng phân tích tế bào máu",
                        0, new BigDecimal("42000"), new BigDecimal("85000"),
                        "42,000", "85,000", "85,000", "85,000", "", "Phòng Xét nghiệm", false),
                new DichVuYeuCauManHinh.ServiceItem(
                        5, "CDHA", "X-Quang", "CDHA_XQ_TIMPHOI", "", "Chụp X-quang ngực thẳng",
                        0, new BigDecimal("65000"), new BigDecimal("120000"),
                        "65,000", "120,000", "120,000", "120,000", "", "Phòng CĐHA", false)
        );
    }
}

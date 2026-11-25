package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaDuLieu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class XetNghiemYeuCauTheManHinhTest {

    @BeforeAll
    static void enableHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void initializesEmptyWithoutHardcoded2022MockData() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            XetNghiemYeuCauTheManHinh panel = new XetNghiemYeuCauTheManHinh(null, KhamBenhGhepNoiGoc.createDefault());
            assertEquals("", panel.getHeaderOrderCodeForTest());
            assertEquals(0, panel.getTicketCardCountForTest());

            DefaultTableModel model = panel.getTableModelForTest();
            assertEquals(5, model.getColumnCount());
            String firstCell = String.valueOf(model.getValueAt(0, 0));
            assertTrue(firstCell.contains("Chưa có chỉ định xét nghiệm"));
            assertFalse(firstCell.contains("AMH"));
            assertFalse(firstCell.contains("06/08/2022"));
        });
    }

    @Test
    void rendersFilteredLabOrdersInFiveColumnsAndClearsWhenVisitHasNone() throws Exception {
        AtomicInteger reportedCount = new AtomicInteger(-1);
        SwingUtilities.invokeAndWait(() -> {
            XetNghiemYeuCauTheManHinh panel = new XetNghiemYeuCauTheManHinh(null, KhamBenhGhepNoiGoc.createDefault());
            panel.setOnLabOrderCountChanged(reportedCount::set);
            panel.setPatientContext(new AdminPatientItem(
                    "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4",
                    true, "Nam", "2022", "4 tuổi", "4", "2022-10-20",
                    "Bắc Ninh", "Bắc Ninh", "0912345002", "BA-2020",
                    "09:10 18/11/2026", "Khoa Khám Bệnh > PK Tai Mũi Họng", "BS. An",
                    "Viêm họng cấp", "VP-2020", 2, 2020, "CHO_KHAM", "Khám bệnh",
                    "Khoa Khám Bệnh", "PK Tai Mũi Họng", "Kinh", "Trẻ em", "Việt Nam",
                    "", "", 0, ""));

            List<DichVuYeuCauDuLieu> mixed = List.of(
                    new DichVuYeuCauDuLieu(
                            71, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                            2, BigDecimal.ONE, new BigDecimal("110000"), "DA_CO_KET_QUA", null, null, null,
                            14, null, LocalDateTime.of(2026, 11, 18, 9, 20), null, null,
                            List.of(new XetNghiemKetQuaDuLieu(1, 71, "WBC", "6.8", "G/L", "4.0 - 10.0", "Bình thường"))
                    ),
                    new DichVuYeuCauDuLieu(
                            72, 2020, 5, "CDHA_XQ_TIMPHOI", "Chụp X-quang ngực", "CHAN_DOAN_HINH_ANH",
                            2, BigDecimal.ONE, new BigDecimal("150000"), "DA_CHI_DINH", null, null, null,
                            null, null, LocalDateTime.of(2026, 11, 18, 9, 21), null, null, List.of()
                    ),
                    new DichVuYeuCauDuLieu(
                            73, 2020, 4, "XN_GLUCOSE", "Định lượng Glucose máu", "XET_NGHIEM",
                            2, BigDecimal.ONE, new BigDecimal("60000"), "DA_HUY", null, null, null,
                            null, null, LocalDateTime.of(2026, 11, 18, 9, 22), null, null, List.of()
                    )
            );

            List<DichVuYeuCauDuLieu> filtered = XetNghiemYeuCauTheManHinh.filterLabOrders(mixed);
            assertEquals(1, filtered.size());
            panel.applyLoadedOrdersForTest(filtered);

            assertEquals(1, reportedCount.get());
            assertEquals("XN-71", panel.getHeaderOrderCodeForTest());
            assertEquals(1, panel.getTicketCardCountForTest());

            DefaultTableModel model = panel.getTableModelForTest();
            assertTrue(String.valueOf(model.getValueAt(0, 0)).startsWith("GROUP:"));
            assertTrue(String.valueOf(model.getValueAt(1, 0)).contains("WBC"));
            assertEquals("1", model.getValueAt(1, 1));
            assertEquals("6.8 (Bình thường)", model.getValueAt(1, 2));
            assertEquals("G/L", model.getValueAt(1, 3));
            assertEquals("4.0 - 10.0", model.getValueAt(1, 4));

            panel.applyLoadedOrdersForTest(List.of());
            assertEquals(0, reportedCount.get());
            assertEquals("", panel.getHeaderOrderCodeForTest());
            assertEquals(0, panel.getTicketCardCountForTest());
            assertTrue(String.valueOf(model.getValueAt(0, 0)).contains("Chưa có chỉ định xét nghiệm"));
        });
    }

    @Test
    void resolvesCancelableOrderIdsForWholeGroupAndSingleSelectedRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            XetNghiemYeuCauTheManHinh panel = new XetNghiemYeuCauTheManHinh(null, KhamBenhGhepNoiGoc.createDefault());
            List<DichVuYeuCauDuLieu> dayOrders = List.of(
                    new DichVuYeuCauDuLieu(
                            81, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                            2, BigDecimal.ONE, new BigDecimal("85000"), "DA_CHI_DINH",
                            null, null, null, null, null,
                            LocalDateTime.of(2026, 11, 18, 9, 20), null, null, List.of()),
                    new DichVuYeuCauDuLieu(
                            82, 2020, 4, "XN_GLUCOSE", "Định lượng Glucose máu", "XET_NGHIEM",
                            2, BigDecimal.ONE, new BigDecimal("45000"), "DA_CHI_DINH",
                            null, null, null, null, null,
                            LocalDateTime.of(2026, 11, 18, 9, 21), null, null, List.of()),
                    new DichVuYeuCauDuLieu(
                            83, 2020, 18, "XN_HS_URE_CRE", "Định lượng Ure máu", "XET_NGHIEM",
                            2, BigDecimal.ONE, new BigDecimal("40000"), "DA_CO_KET_QUA",
                            null, null, null, null, null,
                            LocalDateTime.of(2026, 11, 18, 9, 22), null, null, List.of())
            );
            panel.applyLoadedOrdersForTest(dayOrders);

            // Hủy toàn bộ phiếu ngày -> lấy 81 và 82, tự loại 83 đã có kết quả
            assertEquals(List.of(81, 82), panel.resolveTargetOrderIdsForCancel(true, -1));
            // Dòng 0 là GROUP, dòng 1 là XN-81, dòng 2 là XN-82, dòng 3 là XN-83
            assertEquals(List.of(82), panel.resolveTargetOrderIdsForCancel(false, 2));
            assertEquals(List.of(), panel.resolveTargetOrderIdsForCancel(false, 3));
        });
    }
}

package vn.clinicmanagement.desktop.modules.examination.components.print;


import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewManHinh;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClinicalOrderPrintPreview Wiring Tests (DOC-02)")
class LamSangYeuCauPrintPreviewKetNoiTest {

    @Test
    @DisplayName("Binds real dynamic clinical order data cleanly")
    void bindsRealDataCleanly() {
        LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData order = new LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData(
                "BN-2026-0001",
                "Hoàng Văn Nam",
                "1992",
                "34",
                "Nam",
                "DN4790011223344",
                "31/12/2026",
                "Phường Bến Nghé, Quận 1, TP. HCM",
                "Phòng Khám Nội 1",
                "J06.9 - Nhiễm khuẩn đường hô hấp trên cấp",
                "BA-2026-0001",
                "PID-9999",
                "VP-8888",
                "Còn 300 ngày",
                "08:30, ngày 07 tháng 11 năm 2026",
                List.of(new LamSangYeuCauPrintPreviewManHinh.OrderItem(
                        "Xét nghiệm máu", "Tổng phân tích tế bào máu", "1", "Lần", "Lấy máu đói"
                ))
        );

        LamSangYeuCauPrintPreviewManHinh panel = new LamSangYeuCauPrintPreviewManHinh(order);
        assertNotNull(panel.getBoundData());
        assertEquals("Hoàng Văn Nam", panel.getBoundData().patientName());
        assertEquals("BN-2026-0001", panel.getBoundData().patientCode());
        assertEquals(1, panel.getBoundData().items().size());
        assertEquals("Tổng phân tích tế bào máu", panel.getBoundData().items().get(0).name());
    }

    @Test
    @DisplayName("Empty state renders cleanly without NPE")
    void emptyStateRendersSafely() {
        LamSangYeuCauPrintPreviewManHinh panel = new LamSangYeuCauPrintPreviewManHinh();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }

    @Test
    @DisplayName("Maps real patient and visit orders into clinical order print data without hardcoded demo items")
    void mapsVisitOrdersAndPaintsWithoutError() {
        KhamBenhManHinh.AdminPatientItem patient = new KhamBenhManHinh.AdminPatientItem(
                "1", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4", true,
                "Nam", "2022", "4 tuổi", "4 tuổi", "2022-10-20",
                "Phường Võ Cường, TP Bắc Ninh", "Phường Võ Cường, TP Bắc Ninh", "0912345002",
                "BA-2020", "09:10 18/11/2026", "Khoa Khám Bệnh", "BS. Tiến",
                "J06 - Viêm đường hô hấp trên", "VP-2020");

        List<DichVuYeuCauDuLieu> orders = List.of(
                new DichVuYeuCauDuLieu(
                        11, 2020, 4, "XN_GLUCOSE", "Định lượng Glucose máu", "XET_NGHIEM",
                        2, BigDecimal.ONE, new BigDecimal("45000"), "DA_CHI_DINH",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 15), null, null, List.of()),
                new DichVuYeuCauDuLieu(
                        12, 2020, 5, "CDHA_XQ_TIMPHOI", "Chụp X-quang ngực thẳng (1 phim)", "CDHA",
                        2, BigDecimal.ONE, new BigDecimal("120000"), "DA_CHI_DINH",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 15), null, null, List.of()),
                new DichVuYeuCauDuLieu(
                        13, 2020, 1, "DV_KHAM_NHI", "Khám Nhi chuyên khoa", "KHAM_BENH",
                        2, BigDecimal.ONE, new BigDecimal("120000"), "DA_CHI_DINH",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 10), null, null, List.of()),
                new DichVuYeuCauDuLieu(
                        14, 2020, 3, "XN_CTM", "Tổng phân tích tế bào máu", "XET_NGHIEM",
                        2, BigDecimal.ONE, new BigDecimal("85000"), "DA_HUY",
                        null, null, null, null, null,
                        LocalDateTime.of(2026, 11, 18, 9, 15), null, null, List.of())
        );

        LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData data =
                LamSangYeuCauPrintPreviewManHinh.fromVisitOrders(
                        patient, orders, Map.of(4, "Hóa sinh", 5, "X-Quang"));

        assertEquals("TRẦN MINH KHÔI", data.patientName());
        assertEquals("Viện phí", data.daysLeft());
        assertEquals("", data.patientBhyt());
        assertEquals(2, data.items().size());
        assertEquals("PHÒNG XÉT NGHIỆM - Hóa sinh", data.items().get(0).group());
        assertEquals("PHÒNG CHẨN ĐOÁN HÌNH ẢNH - X-Quang", data.items().get(1).group());

        LamSangYeuCauPrintPreviewManHinh panel = new LamSangYeuCauPrintPreviewManHinh(data);
        panel.setSize(1000, 800);
        BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
        assertDoesNotThrow(() -> panel.paintAll(img.createGraphics()));
    }
}

package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClinicalOrderPrintPreview Wiring Tests (DOC-02)")
class ClinicalOrderPrintPreviewWiringTest {

    @Test
    @DisplayName("Binds real dynamic clinical order data cleanly")
    void bindsRealDataCleanly() {
        ClinicalOrderPrintPreviewPanel.ClinicalOrderData order = new ClinicalOrderPrintPreviewPanel.ClinicalOrderData(
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
                List.of(new ClinicalOrderPrintPreviewPanel.OrderItem(
                        "Xét nghiệm máu", "Tổng phân tích tế bào máu", "1", "Lần", "Lấy máu đói"
                ))
        );

        ClinicalOrderPrintPreviewPanel panel = new ClinicalOrderPrintPreviewPanel(order);
        assertNotNull(panel.getBoundData());
        assertEquals("Hoàng Văn Nam", panel.getBoundData().patientName());
        assertEquals("BN-2026-0001", panel.getBoundData().patientCode());
        assertEquals(1, panel.getBoundData().items().size());
        assertEquals("Tổng phân tích tế bào máu", panel.getBoundData().items().get(0).name());
    }

    @Test
    @DisplayName("Empty state renders cleanly without NPE")
    void emptyStateRendersSafely() {
        ClinicalOrderPrintPreviewPanel panel = new ClinicalOrderPrintPreviewPanel();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }
}

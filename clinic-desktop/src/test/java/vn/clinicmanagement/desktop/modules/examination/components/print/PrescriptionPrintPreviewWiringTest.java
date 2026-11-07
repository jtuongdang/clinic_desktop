package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrescriptionPrintPreview Wiring Tests (DOC-01)")
class PrescriptionPrintPreviewWiringTest {

    @Test
    @DisplayName("Binds real dynamic prescription data cleanly")
    void bindsRealDataCleanly() {
        PrescriptionPrintPreviewPanel.PrescriptionData sample = new PrescriptionPrintPreviewPanel.PrescriptionData(
                "Nguyễn Văn An",
                "BN-2026-9999",
                "1985",
                "Nam",
                "DN4791234567890",
                "Viêm phế quản cấp",
                List.of(new PrescriptionPrintPreviewPanel.PrescriptionItem(
                        "Paracetamol 500mg", "20", "Viên", "Uống 1 viên khi sốt > 38.5C"
                )),
                "BS. CK1 Lê Văn Bình"
        );

        PrescriptionPrintPreviewPanel panel = new PrescriptionPrintPreviewPanel(sample);
        assertNotNull(panel.getBoundData());
        assertEquals("Nguyễn Văn An", panel.getBoundData().patientName());
        assertEquals("BN-2026-9999", panel.getBoundData().patientCode());
        assertEquals(1, panel.getBoundData().medicines().size());
        assertEquals("Paracetamol 500mg", panel.getBoundData().medicines().get(0).name());
    }

    @Test
    @DisplayName("Empty state renders cleanly without NPE")
    void emptyStateRendersSafely() {
        PrescriptionPrintPreviewPanel panel = new PrescriptionPrintPreviewPanel();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }
}

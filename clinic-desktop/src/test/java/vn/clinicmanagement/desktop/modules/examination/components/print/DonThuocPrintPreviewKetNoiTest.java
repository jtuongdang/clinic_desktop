package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrescriptionPrintPreview Wiring Tests (DOC-01)")
class DonThuocPrintPreviewKetNoiTest {

    @Test
    @DisplayName("Binds real dynamic prescription data cleanly")
    void bindsRealDataCleanly() {
        DonThuocPrintPreviewManHinh.PrescriptionData sample = new DonThuocPrintPreviewManHinh.PrescriptionData(
                "Nguyễn Văn An",
                "BN-2026-9999",
                "1985",
                "Nam",
                "DN4791234567890",
                "Viêm phế quản cấp",
                List.of(new DonThuocPrintPreviewManHinh.PrescriptionItem(
                        "Paracetamol 500mg", "20", "Viên", "Uống 1 viên khi sốt > 38.5C"
                )),
                "BS. CK1 Lê Văn Bình"
        );

        DonThuocPrintPreviewManHinh panel = new DonThuocPrintPreviewManHinh(sample);
        assertNotNull(panel.getBoundData());
        assertEquals("Nguyễn Văn An", panel.getBoundData().patientName());
        assertEquals("BN-2026-9999", panel.getBoundData().patientCode());
        assertEquals(1, panel.getBoundData().medicines().size());
        assertEquals("Paracetamol 500mg", panel.getBoundData().medicines().get(0).name());
    }

    @Test
    @DisplayName("Empty state renders cleanly without NPE")
    void emptyStateRendersSafely() {
        DonThuocPrintPreviewManHinh panel = new DonThuocPrintPreviewManHinh();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }
}

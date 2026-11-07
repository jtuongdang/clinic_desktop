package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MedicalRecordPrintPreview Wiring Tests (DOC-01)")
class MedicalRecordPrintPreviewWiringTest {

    @Test
    @DisplayName("Binds real medical record data cleanly")
    void bindsRealMedicalRecordCleanly() {
        MedicalRecordPrintPreviewPanel.MedicalRecordData record = new MedicalRecordPrintPreviewPanel.MedicalRecordData(
                "Trần Thị Mai",
                "BN-2026-8888",
                "1990",
                "Nữ",
                "GD4799876543210",
                "Quận 1, TP. Hồ Chí Minh",
                "BA-2026-00123",
                "Đau họng, sốt nhẹ",
                "Bệnh 2 ngày nay",
                "Không dị ứng thuốc",
                "Bình thường",
                "Họng đỏ, niêm mạc xung huyết",
                "Viêm họng cấp",
                "Kháng sinh, kháng viêm, súc họng"
        );

        MedicalRecordPrintPreviewPanel panel = new MedicalRecordPrintPreviewPanel(record);
        assertNotNull(panel.getBoundData());
        assertEquals("Trần Thị Mai", panel.getBoundData().patientName());
        assertEquals("BN-2026-8888", panel.getBoundData().patientCode());
        assertEquals("Viêm họng cấp", panel.getBoundData().diagnosis());
    }

    @Test
    @DisplayName("Empty state renders cleanly without throwing exception")
    void emptyStateRendersSafely() {
        MedicalRecordPrintPreviewPanel panel = new MedicalRecordPrintPreviewPanel();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }
}

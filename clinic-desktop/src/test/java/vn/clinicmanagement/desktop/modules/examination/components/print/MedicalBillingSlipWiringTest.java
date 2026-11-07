package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MedicalBillingSlip Wiring Tests (DOC-02)")
class MedicalBillingSlipWiringTest {

    @Test
    @DisplayName("Binds real dynamic billing slip data cleanly")
    void bindsRealBillingSlipCleanly() {
        MedicalBillingSlipPanel.BillingSlipData data = new MedicalBillingSlipPanel.BillingSlipData(
                "Lê Thị Hoa",
                "BN-2026-0002",
                "1998",
                "28",
                "Nữ",
                "VP-9988",
                "GD4790088776655",
                "Còn 180 ngày",
                "Quận Bình Thạnh, TP. HCM",
                "80%",
                BigDecimal.valueOf(1_500_000),
                BigDecimal.valueOf(1_200_000),
                BigDecimal.valueOf(300_000)
        );

        MedicalBillingSlipPanel panel = new MedicalBillingSlipPanel(data);
        assertNotNull(panel.getBoundData());
        assertEquals("Lê Thị Hoa", panel.getBoundData().patientName());
        assertEquals("BN-2026-0002", panel.getBoundData().patientCode());
        assertEquals("VP-9988", panel.getBoundData().feeCode());
    }

    @Test
    @DisplayName("Empty state renders cleanly without exception")
    void emptyStateRendersSafely() {
        MedicalBillingSlipPanel panel = new MedicalBillingSlipPanel();
        assertNull(panel.getBoundData());
        assertDoesNotThrow(() -> panel.bindData(null));
    }
}

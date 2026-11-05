package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceptionPanelMessagePolicyTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void knownRegistrationCodesMapToFixedVietnameseMessages() {
        assertTrue(GraphicsEnvironment.isHeadless(), "Message policy tests must run in headless mode");
        assertEquals(
                "Thông tin người bệnh chưa hợp lệ. Vui lòng kiểm tra lại các trường bắt buộc.",
                ReceptionPanel.registrationFailureMessage("PATIENT_VALIDATION"));
        assertEquals(
                "Phòng khám đã chọn không còn khả dụng. Vui lòng chọn phòng khám khác.",
                ReceptionPanel.registrationFailureMessage("ROOM_UNAVAILABLE"));
        assertEquals(
                "Không thể hoàn tất đăng ký người bệnh. Vui lòng thử lại.",
                ReceptionPanel.registrationFailureMessage("PATIENT_REGISTRATION_FAILED"));
    }

    @Test
    void unknownAndNullCodesUseGenericMessageWithoutEchoingInput() {
        String fallback = "Đã xảy ra lỗi khi đăng ký người bệnh. Vui lòng thử lại sau.";
        String syntheticExceptionMessage = "Synthetic SQLException payload";

        assertEquals(fallback, ReceptionPanel.registrationFailureMessage(null));
        assertEquals(fallback, ReceptionPanel.registrationFailureMessage("UNKNOWN_SYNTHETIC_CODE"));
        String mappedPayload = ReceptionPanel.registrationFailureMessage(syntheticExceptionMessage);
        assertEquals(fallback, mappedPayload);
        assertFalse(mappedPayload.contains(syntheticExceptionMessage));
    }
}

package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionPanelMessagePolicyTest {

    @Test
    void duplicateCccdReturnsUserMessageWithMaskedCccdAndSearchPrompt() {
        String rawCccd = "001012345601";
        String message = ReceptionPanelMessagePolicy.toUserMessage("PATIENT_DUPLICATE_CCCD", rawCccd);

        assertNotNull(message);
        assertTrue(message.contains("0010******01"),
                "Expected masked CCCD '0010******01' but got: " + message);
        assertTrue(message.contains("tìm kiếm") || message.contains("tra cứu"),
                "Expected prompt to search existing record but got: " + message);

        // Also test direct helper
        String directMessage = ReceptionPanelMessagePolicy.toDuplicateCccdMessage(rawCccd);
        assertTrue(directMessage.contains("0010******01"));
        assertTrue(directMessage.contains("tìm kiếm") || directMessage.contains("tra cứu"));

        // When no CCCD is provided
        String noCccdMessage = ReceptionPanelMessagePolicy.toUserMessage("PATIENT_DUPLICATE_CCCD");
        assertNotNull(noCccdMessage);
        assertTrue(noCccdMessage.contains("tìm kiếm") || noCccdMessage.contains("tra cứu"));
    }

    @Test
    void cccdMaskingBehavior() {
        assertEquals("0010******01", ReceptionPanelMessagePolicy.maskCccd("001012345601"));
        assertEquals("1234***89", ReceptionPanelMessagePolicy.maskCccd("123456789"));
        assertEquals("******", ReceptionPanelMessagePolicy.maskCccd("12345"));
        assertEquals("******", ReceptionPanelMessagePolicy.maskCccd(""));
        assertEquals("******", ReceptionPanelMessagePolicy.maskCccd(null));
    }

    @Test
    void roomUnavailableDisplaysStandardVietnameseMessage() {
        String message = ReceptionPanelMessagePolicy.toUserMessage("ROOM_UNAVAILABLE");
        assertEquals("Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.", message);

        Result<?> failureResult = Result.failure("ROOM_UNAVAILABLE", "Internal room error");
        assertEquals("Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.",
                ReceptionPanelMessagePolicy.toUserMessage(failureResult));
    }

    @Test
    void authorizationDeniedDisplaysSafeMessageWithoutInternalTokens() {
        String message = ReceptionPanelMessagePolicy.toUserMessage("AUTHORIZATION_DENIED");
        assertNotNull(message);
        assertFalse(message.contains("ROLE_"), "Message must not leak internal permission tokens");
        assertFalse(message.contains("ACTION_"), "Message must not leak internal permission tokens");
        assertFalse(message.contains("MODULE_"), "Message must not leak internal permission tokens");
        assertTrue(message.contains("Không được phép") || message.contains("không có quyền"));

        assertEquals(message, ReceptionPanelMessagePolicy.toUserMessage("AUTH_FORBIDDEN"));
        assertEquals(message, ReceptionPanelMessagePolicy.toUserMessage("AUTH_ACCESS_DENIED"));
    }

    @Test
    void patientRegistrationFailedDisplaysGenericSafeRetryMessage() {
        String message = ReceptionPanelMessagePolicy.toUserMessage("PATIENT_REGISTRATION_FAILED");
        assertEquals("Đăng ký tiếp đón không thành công. Vui lòng kiểm tra lại thông tin và thử lại.", message);
    }

    @Test
    void patientSearchAndQueueAndCatalogMessages() {
        assertEquals("Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.",
                ReceptionPanelMessagePolicy.toUserMessage("SEARCH_KEYWORD_REQUIRED"));
        assertEquals("Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.",
                ReceptionPanelMessagePolicy.toUserMessage("PATIENT_SEARCH_FAILED"));
        assertEquals("Không thể tải hàng đợi tiếp đón.",
                ReceptionPanelMessagePolicy.toUserMessage("RECEPTION_QUEUE_FAILED"));
        assertEquals("Không thể tải danh sách phòng khám.",
                ReceptionPanelMessagePolicy.toUserMessage("ROOM_CATALOG_FAILED"));
        assertEquals("Giao dịch không thành công. Dữ liệu đã được hoàn tác an toàn.",
                ReceptionPanelMessagePolicy.toUserMessage("TRANSACTION_FAILED"));
        assertEquals("Thao tác ngoài phạm vi cơ sở hoặc khoa phòng được phân công.",
                ReceptionPanelMessagePolicy.toUserMessage("SCOPE_MISMATCH"));
    }

    @Test
    void patientValidationSanitization() {
        // Safe field message
        String fieldMsg = "Họ và tên bệnh nhân không được để trống.";
        assertEquals(fieldMsg, ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION", fieldMsg));

        // Unsafe message containing SQL / exception keywords is masked to generic validation failure
        String unsafeMsg = "SQLException: SELECT * FROM BenhNhan WHERE id=1 failed";
        String sanitized = ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION", unsafeMsg);
        assertFalse(sanitized.contains("SQLException"));
        assertFalse(sanitized.contains("SELECT"));
        assertFalse(sanitized.contains("BenhNhan"));
        assertEquals("Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.", sanitized);
    }

    @Test
    void successResultReturnsEmptyUserMessage() {
        Result<String> success = Result.success("ok");
        assertEquals("", ReceptionPanelMessagePolicy.toUserMessage(success));
        assertEquals("", ReceptionPanelMessagePolicy.toUserMessage((Result<?>) null));
    }
}

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
    void scopeMismatchErrorCodesMapToStandardMessage() {
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage("SCOPE_MISMATCH"));
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage("TARGET_MISMATCH"));
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage("AUTH_SCOPE_MISMATCH"));
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage("OUT_OF_SCOPE"));

        // Verify via Result.failure
        Result<?> authScopeFailure = Result.failure("AUTH_SCOPE_MISMATCH", "User facility scope mismatch");
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage(authScopeFailure));

        Result<?> outOfScopeFailure = Result.failure("OUT_OF_SCOPE", "Target department out of assigned scope");
        assertEquals(ReceptionPanelMessagePolicy.MSG_SCOPE_MISMATCH,
                ReceptionPanelMessagePolicy.toUserMessage(outOfScopeFailure));
    }

    @Test
    void isSafeMessageBlocksTechnicalCredentialAndIdentifierTokens() {
        // Credential and secret tokens
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("password"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("User Password Required"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("matkhau"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("MatKhau khong dung"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("token"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("Bearer TOKEN expired"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("secret"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("Invalid client_SECRET"));

        // Identifier tokens
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("cccd"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("cccd 001090000001"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("Kiem tra so CCCD"));

        // Technical / infrastructure tokens
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("jdbc:"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("jdbc:sqlserver://localhost:1433"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("JDBC:mssql connection failed"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("connection"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("database Connection closed"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("stacktrace"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("Error StackTrace at java.lang.Thread"));

        // Existing SQL / database tokens
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("sql"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("select"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("insert"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("update"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("delete"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("table"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("column"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("nhatky"));
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage("exception"));

        // Null check
        assertFalse(ReceptionPanelMessagePolicy.isSafeMessage(null));

        // Safe message passes
        assertTrue(ReceptionPanelMessagePolicy.isSafeMessage("Họ và tên bệnh nhân không được để trống."));
        assertTrue(ReceptionPanelMessagePolicy.isSafeMessage("Ngày sinh không được để trống."));
        assertTrue(ReceptionPanelMessagePolicy.isSafeMessage("Vui lòng chọn phòng khám hợp lệ."));
    }

    @Test
    void patientValidationSanitizesSensitiveAndTechnicalTokens() {
        String safeValidationMsg = "Số điện thoại phải gồm 10 chữ số.";
        assertEquals(safeValidationMsg,
                ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION", safeValidationMsg));

        String[] sensitiveDetails = {
                "jdbc:sqlserver://localhost:1433;db=clinic",
                "User password is required",
                "cccd 001090000001 format is invalid",
                "matkhau chua du manh",
                "auth token revoked",
                "app secret invalid",
                "database connection failed",
                "full stacktrace in server log",
                "SQLException: table BenhNhan not found"
        };

        for (String unsafeDetail : sensitiveDetails) {
            String sanitized = ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION", unsafeDetail);
            assertEquals("Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.", sanitized,
                    "Validation detail should be sanitized when containing: " + unsafeDetail);
            assertFalse(sanitized.contains(unsafeDetail));
        }
    }

    @Test
    void unknownErrorCodeSanitizesSensitiveAndTechnicalTokens() {
        // Unknown error code with safe detail passes through
        String safeDetail = "Mã thẻ bảo hiểm không hợp lệ.";
        assertEquals(safeDetail, ReceptionPanelMessagePolicy.toUserMessage("CUSTOM_WARNING_CODE", safeDetail));

        // Unknown error code with sensitive details gets sanitized to MSG_DEFAULT_ERROR
        String[] sensitiveDetails = {
                "jdbc:sqlserver://localhost:1433;databaseName=clinicDb;user=admin",
                "Invalid password provided for user admin",
                "Duplicate cccd 001090000001 found in records",
                "matkhau nguoi dung bi lo",
                "Bearer token authentication rejected",
                "client secret verification failed",
                "HikariCP connection pool exhausted",
                "NullPointerException stacktrace at vn.clinic.App",
                "SQL syntax error in select statement"
        };

        for (String unsafeDetail : sensitiveDetails) {
            String sanitized = ReceptionPanelMessagePolicy.toUserMessage("UNKNOWN_RUNTIME_ERROR", unsafeDetail);
            assertEquals(ReceptionPanelMessagePolicy.MSG_DEFAULT_ERROR, sanitized,
                    "Unknown error detail should be masked to default error when containing: " + unsafeDetail);
            assertFalse(sanitized.contains(unsafeDetail));
        }
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

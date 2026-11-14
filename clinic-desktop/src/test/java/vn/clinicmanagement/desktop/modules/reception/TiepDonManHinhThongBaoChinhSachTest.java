package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonManHinhThongBaoChinhSachTest {

    @Test
    void duplicateCccdReturnsUserMessageWithMaskedCccdAndSearchPrompt() {
        String rawCccd = "001012345601";
        String message = TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_DUPLICATE_CCCD", rawCccd);

        assertNotNull(message);
        assertTrue(message.contains("0010******01"),
                "Expected masked CCCD '0010******01' but got: " + message);
        assertTrue(message.contains("tìm kiếm") || message.contains("tra cứu"),
                "Expected prompt to search existing record but got: " + message);

        // Also test direct helper
        String directMessage = TiepDonManHinhThongBaoChinhSach.toDuplicateCccdMessage(rawCccd);
        assertTrue(directMessage.contains("0010******01"));
        assertTrue(directMessage.contains("tìm kiếm") || directMessage.contains("tra cứu"));

        // When no CCCD is provided
        String noCccdMessage = TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_DUPLICATE_CCCD");
        assertNotNull(noCccdMessage);
        assertTrue(noCccdMessage.contains("tìm kiếm") || noCccdMessage.contains("tra cứu"));
    }

    @Test
    void cccdMaskingBehavior() {
        assertEquals("0010******01", TiepDonManHinhThongBaoChinhSach.maskCccd("001012345601"));
        assertEquals("1234***89", TiepDonManHinhThongBaoChinhSach.maskCccd("123456789"));
        assertEquals("******", TiepDonManHinhThongBaoChinhSach.maskCccd("12345"));
        assertEquals("******", TiepDonManHinhThongBaoChinhSach.maskCccd(""));
        assertEquals("******", TiepDonManHinhThongBaoChinhSach.maskCccd(null));
    }

    @Test
    void roomUnavailableDisplaysStandardVietnameseMessage() {
        String message = TiepDonManHinhThongBaoChinhSach.toUserMessage("ROOM_UNAVAILABLE");
        assertEquals("Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.", message);

        KetQua<?> failureResult = KetQua.failure("ROOM_UNAVAILABLE", "Internal room error");
        assertEquals("Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage(failureResult));
    }

    @Test
    void authorizationDeniedDisplaysSafeMessageWithoutInternalTokens() {
        String message = TiepDonManHinhThongBaoChinhSach.toUserMessage("AUTHORIZATION_DENIED");
        assertNotNull(message);
        assertFalse(message.contains("ROLE_"), "Message must not leak internal permission tokens");
        assertFalse(message.contains("ACTION_"), "Message must not leak internal permission tokens");
        assertFalse(message.contains("MODULE_"), "Message must not leak internal permission tokens");
        assertTrue(message.contains("Không được phép") || message.contains("không có quyền"));

        assertEquals(message, TiepDonManHinhThongBaoChinhSach.toUserMessage("AUTH_FORBIDDEN"));
        assertEquals(message, TiepDonManHinhThongBaoChinhSach.toUserMessage("AUTH_ACCESS_DENIED"));
    }

    @Test
    void patientRegistrationFailedDisplaysGenericSafeRetryMessage() {
        String message = TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_REGISTRATION_FAILED");
        assertEquals("Đăng ký tiếp đón không thành công. Vui lòng kiểm tra lại thông tin và thử lại.", message);
    }

    @Test
    void patientSearchAndQueueAndCatalogMessages() {
        assertEquals("Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("SEARCH_KEYWORD_REQUIRED"));
        assertEquals("Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_SEARCH_FAILED"));
        assertEquals("Không thể tải hàng đợi tiếp đón.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("RECEPTION_QUEUE_FAILED"));
        assertEquals("Không thể tải danh sách phòng khám.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("ROOM_CATALOG_FAILED"));
        assertEquals("Giao dịch không thành công. Dữ liệu đã được hoàn tác an toàn.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("TRANSACTION_FAILED"));
        assertEquals("Thao tác ngoài phạm vi cơ sở hoặc khoa phòng được phân công.",
                TiepDonManHinhThongBaoChinhSach.toUserMessage("SCOPE_MISMATCH"));
    }

    @Test
    void scopeMismatchErrorCodesMapToStandardMessage() {
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage("SCOPE_MISMATCH"));
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage("TARGET_MISMATCH"));
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage("AUTH_SCOPE_MISMATCH"));
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage("OUT_OF_SCOPE"));

        // Verify via Result.failure
        KetQua<?> authScopeFailure = KetQua.failure("AUTH_SCOPE_MISMATCH", "User facility scope mismatch");
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage(authScopeFailure));

        KetQua<?> outOfScopeFailure = KetQua.failure("OUT_OF_SCOPE", "Target department out of assigned scope");
        assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_SCOPE_MISMATCH,
                TiepDonManHinhThongBaoChinhSach.toUserMessage(outOfScopeFailure));
    }

    @Test
    void isSafeMessageBlocksTechnicalCredentialAndIdentifierTokens() {
        // Credential and secret tokens
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("password"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("User Password Required"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("matkhau"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("MatKhau khong dung"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("token"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Bearer TOKEN expired"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("secret"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Invalid client_SECRET"));

        // Identifier tokens
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("cccd"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("cccd 001090000001"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Kiem tra so CCCD"));

        // Technical / infrastructure tokens
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("jdbc:"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("jdbc:sqlserver://localhost:1433"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("JDBC:mssql connection failed"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("connection"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("database Connection closed"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("stacktrace"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Error StackTrace at java.lang.Thread"));

        // Existing SQL / database tokens
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("sql"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("select"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("insert"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("update"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("delete"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("table"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("column"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("nhatky"));
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage("exception"));

        // Null check
        assertFalse(TiepDonManHinhThongBaoChinhSach.isSafeMessage(null));

        // Safe message passes
        assertTrue(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Họ và tên bệnh nhân không được để trống."));
        assertTrue(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Ngày sinh không được để trống."));
        assertTrue(TiepDonManHinhThongBaoChinhSach.isSafeMessage("Vui lòng chọn phòng khám hợp lệ."));
    }

    @Test
    void patientValidationSanitizesSensitiveAndTechnicalTokens() {
        String safeValidationMsg = "Số điện thoại phải gồm 10 chữ số.";
        assertEquals(safeValidationMsg,
                TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION", safeValidationMsg));

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
            String sanitized = TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION", unsafeDetail);
            assertEquals("Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.", sanitized,
                    "Validation detail should be sanitized when containing: " + unsafeDetail);
            assertFalse(sanitized.contains(unsafeDetail));
        }
    }

    @Test
    void unknownErrorCodeSanitizesSensitiveAndTechnicalTokens() {
        // Unknown error code with safe detail passes through
        String safeDetail = "Mã thẻ bảo hiểm không hợp lệ.";
        assertEquals(safeDetail, TiepDonManHinhThongBaoChinhSach.toUserMessage("CUSTOM_WARNING_CODE", safeDetail));

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
            String sanitized = TiepDonManHinhThongBaoChinhSach.toUserMessage("UNKNOWN_RUNTIME_ERROR", unsafeDetail);
            assertEquals(TiepDonManHinhThongBaoChinhSach.MSG_DEFAULT_ERROR, sanitized,
                    "Unknown error detail should be masked to default error when containing: " + unsafeDetail);
            assertFalse(sanitized.contains(unsafeDetail));
        }
    }

    @Test
    void patientValidationSanitization() {
        // Safe field message
        String fieldMsg = "Họ và tên bệnh nhân không được để trống.";
        assertEquals(fieldMsg, TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION", fieldMsg));

        // Unsafe message containing SQL / exception keywords is masked to generic validation failure
        String unsafeMsg = "SQLException: SELECT * FROM BenhNhan WHERE id=1 failed";
        String sanitized = TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION", unsafeMsg);
        assertFalse(sanitized.contains("SQLException"));
        assertFalse(sanitized.contains("SELECT"));
        assertFalse(sanitized.contains("BenhNhan"));
        assertEquals("Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.", sanitized);
    }

    @Test
    void successResultReturnsEmptyUserMessage() {
        KetQua<String> success = KetQua.success("ok");
        assertEquals("", TiepDonManHinhThongBaoChinhSach.toUserMessage(success));
        assertEquals("", TiepDonManHinhThongBaoChinhSach.toUserMessage((KetQua<?>) null));
    }
}

package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.common.Result;

import java.util.Locale;
import java.util.Objects;

/**
 * Translates application business error codes to sanitized, localized
 * Vietnamese user-facing messages for the reception workstation.
 */
public final class ReceptionPanelMessagePolicy {

    public static final String MSG_ROOM_UNAVAILABLE =
            "Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.";
    public static final String MSG_AUTHORIZATION_DENIED =
            "Không được phép thực hiện thao tác trong phiên làm việc hiện tại.";
    public static final String MSG_PATIENT_REGISTRATION_FAILED =
            "Đăng ký tiếp đón không thành công. Vui lòng kiểm tra lại thông tin và thử lại.";
    public static final String MSG_PATIENT_SEARCH_FAILED =
            "Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    public static final String MSG_RECEPTION_QUEUE_FAILED =
            "Không thể tải hàng đợi tiếp đón.";
    public static final String MSG_ROOM_CATALOG_FAILED =
            "Không thể tải danh sách phòng khám.";
    public static final String MSG_SEARCH_KEYWORD_REQUIRED =
            "Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.";
    public static final String MSG_TRANSACTION_FAILED =
            "Giao dịch không thành công. Dữ liệu đã được hoàn tác an toàn.";
    public static final String MSG_SCOPE_MISMATCH =
            "Thao tác ngoài phạm vi cơ sở hoặc khoa phòng được phân công.";
    public static final String MSG_DEFAULT_ERROR =
            "Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.";

    private ReceptionPanelMessagePolicy() {}

    /**
     * Masks CCCD for safe user presentation: retains the first 4 and last 2 characters,
     * replacing intermediate digits with asterisks (e.g. 0010******01).
     */
    public static String maskCccd(String cccd) {
        if (cccd == null || cccd.isBlank()) {
            return "******";
        }
        String trimmed = cccd.trim();
        if (trimmed.length() <= 6) {
            return "******";
        }
        int prefixLen = 4;
        int suffixLen = 2;
        int maskLen = trimmed.length() - prefixLen - suffixLen;
        return trimmed.substring(0, prefixLen) + "*".repeat(maskLen) + trimmed.substring(trimmed.length() - suffixLen);
    }

    /**
     * Generates localized duplicate CCCD prompt with masked identifier.
     */
    public static String toDuplicateCccdMessage(String cccd) {
        String masked = maskCccd(cccd);
        return "Số CCCD " + masked + " đã tồn tại trong hệ thống. Bạn có muốn tìm kiếm hồ sơ bệnh nhân hiện có không?";
    }

    public static String toUserMessage(String errorCode) {
        return toUserMessage(errorCode, null);
    }

    public static String toUserMessage(String errorCode, String detailOrIdentifier) {
        if (errorCode == null || errorCode.isBlank()) {
            return MSG_DEFAULT_ERROR;
        }
        return switch (errorCode.trim()) {
            case "PATIENT_DUPLICATE_CCCD", "PATIENT_IDENTITY_CONFLICT" ->
                    toDuplicateCccdMessage(detailOrIdentifier);
            case "ROOM_UNAVAILABLE" ->
                    MSG_ROOM_UNAVAILABLE;
            case "AUTHORIZATION_DENIED", "AUTH_FORBIDDEN", "AUTH_ACCESS_DENIED" ->
                    MSG_AUTHORIZATION_DENIED;
            case "PATIENT_REGISTRATION_FAILED" ->
                    MSG_PATIENT_REGISTRATION_FAILED;
            case "PATIENT_SEARCH_FAILED" ->
                    MSG_PATIENT_SEARCH_FAILED;
            case "RECEPTION_QUEUE_FAILED" ->
                    MSG_RECEPTION_QUEUE_FAILED;
            case "ROOM_CATALOG_FAILED" ->
                    MSG_ROOM_CATALOG_FAILED;
            case "SEARCH_KEYWORD_REQUIRED" ->
                    MSG_SEARCH_KEYWORD_REQUIRED;
            case "TRANSACTION_FAILED", "TRANSACTION_ROLLBACK_ONLY" ->
                    MSG_TRANSACTION_FAILED;
            case "SCOPE_MISMATCH", "TARGET_MISMATCH", "AUTH_SCOPE_MISMATCH", "OUT_OF_SCOPE" ->
                    MSG_SCOPE_MISMATCH;
            case "PATIENT_VALIDATION" -> {
                if (detailOrIdentifier != null && !detailOrIdentifier.isBlank()) {
                    yield sanitizeValidationMessage(detailOrIdentifier);
                }
                yield "Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.";
            }
            default -> {
                if (detailOrIdentifier != null && !detailOrIdentifier.isBlank() && isSafeMessage(detailOrIdentifier)) {
                    yield detailOrIdentifier;
                }
                yield MSG_DEFAULT_ERROR;
            }
        };
    }

    public static String toUserMessage(Result<?> result) {
        if (result == null || result.isSuccess()) {
            return "";
        }
        return result.getError()
                .map(err -> toUserMessage(err.code(), err.message()))
                .orElse(MSG_DEFAULT_ERROR);
    }

    private static String sanitizeValidationMessage(String raw) {
        if (!isSafeMessage(raw)) {
            return "Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.";
        }
        return raw;
    }

    static boolean isSafeMessage(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("exception")
                && !lower.contains("sql")
                && !lower.contains("select")
                && !lower.contains("insert")
                && !lower.contains("update")
                && !lower.contains("delete")
                && !lower.contains("table")
                && !lower.contains("column")
                && !lower.contains("nhatky")
                && !lower.contains("password")
                && !lower.contains("matkhau")
                && !lower.contains("token")
                && !lower.contains("secret")
                && !lower.contains("cccd")
                && !lower.contains("jdbc:")
                && !lower.contains("connection")
                && !lower.contains("stacktrace");
    }
}

package vn.clinicmanagement.desktop.core.diagnostics;

import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;

import java.util.UUID;

/** Chẩn đoán an toàn cho các lỗi tác vụ nền desktop đã bị loại bỏ. */
public final class MayTinhTacVuNenChanDoanLoi {
    private MayTinhTacVuNenChanDoanLoi() {}

    public static void logLoginAuthenticationFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_LOGIN_AUTHENTICATION", "LOGIN_AUTHENTICATION_FAILED", failure);
    }

    public static void logLoginSessionCompletionFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_LOGIN_SESSION_COMPLETION", "LOGIN_SESSION_COMPLETION_FAILED", failure);
    }

    public static void logDashboardReauthenticationFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_DASHBOARD_REAUTHENTICATION",
                "DASHBOARD_REAUTHENTICATION_FAILED", failure);
    }

    public static void restoreInterruptStatus() {
        Thread.currentThread().interrupt();
    }

    private static void logUnexpectedFailure(String operationCode, String resultCode, Throwable failure) {
        UngDungThatBaiChanDoanLoi.logUnexpectedFailure(
                operationCode, resultCode, UUID.randomUUID().toString(), failure);
    }
}

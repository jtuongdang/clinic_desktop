package vn.clinicmanagement.desktop.core.diagnostics;

import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;

import java.util.UUID;

/** Safe diagnostics for discarded desktop worker failures. */
public final class MayTinhTacVuNenChanDoanLoi {
    private MayTinhTacVuNenChanDoanLoi() {}

    /** Hàm `logLoginAuthenticationFailure`: nhatky dangnhap xác thực thất bại. */
    public static void logLoginAuthenticationFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_LOGIN_AUTHENTICATION", "LOGIN_AUTHENTICATION_FAILED", failure);
    }

    /** Hàm `logLoginSessionCompletionFailure`: nhatky dangnhap phien `Completion` thất bại. */
    public static void logLoginSessionCompletionFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_LOGIN_SESSION_COMPLETION", "LOGIN_SESSION_COMPLETION_FAILED", failure);
    }

    /** Hàm `logDashboardReauthenticationFailure`: nhatky bangdieukhien `Reauthentication` thất bại. */
    public static void logDashboardReauthenticationFailure(Throwable failure) {
        logUnexpectedFailure("DESKTOP_DASHBOARD_REAUTHENTICATION",
                "DASHBOARD_REAUTHENTICATION_FAILED", failure);
    }

    /** Hàm `restoreInterruptStatus`: `restore` `Interrupt` trạng thái. */
    public static void restoreInterruptStatus() {
        Thread.currentThread().interrupt();
    }

    /** Hàm `logUnexpectedFailure`: nhatky `Unexpected` thất bại. */
    private static void logUnexpectedFailure(String operationCode, String resultCode, Throwable failure) {
        UngDungThatBaiChanDoanLoi.logUnexpectedFailure(
                operationCode, resultCode, UUID.randomUUID().toString(), failure);
    }
}

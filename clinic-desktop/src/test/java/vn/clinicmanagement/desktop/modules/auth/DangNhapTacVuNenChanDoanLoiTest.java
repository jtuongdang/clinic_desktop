package vn.clinicmanagement.desktop.modules.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DangNhapTacVuNenChanDoanLoiTest {
    @Test
    void unexpectedLoginWorkerFailureUsesSafeBoundedDiagnostic() {
        Logger logger = Logger.getLogger(UngDungThatBaiChanDoanLoi.class.getName());
        Level previousLevel = logger.getLevel();
        boolean previousParentHandlers = logger.getUseParentHandlers();
        List<String> diagnostics = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                diagnostics.add(record.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        };

        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            IllegalStateException failure = new IllegalStateException("SYNTHETIC_CREDENTIALS_AND_PAYLOAD");
            MayTinhTacVuNenChanDoanLoi.logLoginAuthenticationFailure(failure);
            MayTinhTacVuNenChanDoanLoi.logLoginSessionCompletionFailure(failure);
            MayTinhTacVuNenChanDoanLoi.logDashboardReauthenticationFailure(failure);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousParentHandlers);
        }

        assertEquals(3, diagnostics.size());
        assertDiagnosticIsSafe(diagnostics.get(0), "DESKTOP_LOGIN_AUTHENTICATION",
                "LOGIN_AUTHENTICATION_FAILED");
        assertDiagnosticIsSafe(diagnostics.get(1), "DESKTOP_LOGIN_SESSION_COMPLETION",
                "LOGIN_SESSION_COMPLETION_FAILED");
        assertDiagnosticIsSafe(diagnostics.get(2), "DESKTOP_DASHBOARD_REAUTHENTICATION",
                "DASHBOARD_REAUTHENTICATION_FAILED");
    }

    private static void assertDiagnosticIsSafe(String diagnostic, String operation, String result) {
        assertTrue(diagnostic.contains("operation=" + operation));
        assertTrue(diagnostic.contains("result=" + result));
        assertTrue(diagnostic.matches(".*correlationId=[0-9a-f-]{36}.*"));
        assertTrue(diagnostic.contains("java.lang.IllegalStateException"));
        assertFalse(diagnostic.contains("SYNTHETIC_CREDENTIALS_AND_PAYLOAD"));
    }
}

package vn.clinicmanagement.desktop.core.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MayTinhTacVuNenChanDoanLoiTest {
    @Test
    void restoreInterruptStatusSetsAndClearsCurrentThreadInterruptFlag() {
        boolean previouslyInterrupted = Thread.interrupted();
        try {
            MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (previouslyInterrupted) Thread.currentThread().interrupt();
        }
    }

    @Test
    void failureLoggingHandlesNonNullExceptions() {
        IllegalStateException failure = new IllegalStateException("synthetic failure");

        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logLoginAuthenticationFailure(failure));
        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logLoginSessionCompletionFailure(failure));
        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logDashboardReauthenticationFailure(failure));
    }

    @Test
    void failureLoggingHandlesNullExceptions() {
        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logLoginAuthenticationFailure(null));
        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logLoginSessionCompletionFailure(null));
        assertDoesNotThrow(() -> MayTinhTacVuNenChanDoanLoi.logDashboardReauthenticationFailure(null));
    }
}

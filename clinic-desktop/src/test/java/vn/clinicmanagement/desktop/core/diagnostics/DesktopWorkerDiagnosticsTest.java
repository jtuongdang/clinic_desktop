package vn.clinicmanagement.desktop.core.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopWorkerDiagnosticsTest {
    @Test
    void restoreInterruptStatusSetsAndClearsCurrentThreadInterruptFlag() {
        boolean previouslyInterrupted = Thread.interrupted();
        try {
            DesktopWorkerDiagnostics.restoreInterruptStatus();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (previouslyInterrupted) Thread.currentThread().interrupt();
        }
    }

    @Test
    void failureLoggingHandlesNonNullExceptions() {
        IllegalStateException failure = new IllegalStateException("synthetic failure");

        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logLoginAuthenticationFailure(failure));
        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logLoginSessionCompletionFailure(failure));
        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logDashboardReauthenticationFailure(failure));
    }

    @Test
    void failureLoggingHandlesNullExceptions() {
        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logLoginAuthenticationFailure(null));
        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logLoginSessionCompletionFailure(null));
        assertDoesNotThrow(() -> DesktopWorkerDiagnostics.logDashboardReauthenticationFailure(null));
    }
}

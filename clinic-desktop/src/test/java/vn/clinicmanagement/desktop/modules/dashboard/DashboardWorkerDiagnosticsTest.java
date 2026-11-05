package vn.clinicmanagement.desktop.modules.dashboard;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardWorkerDiagnosticsTest {
    @Test
    void interruptedWorkerRestoresCurrentThreadInterruptStatus() {
        boolean previouslyInterrupted = Thread.interrupted();
        try {
            DesktopWorkerDiagnostics.restoreInterruptStatus();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (previouslyInterrupted) Thread.currentThread().interrupt();
        }
    }
}

package vn.clinicmanagement.desktop.modules.dashboard;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BangDieuKhienTacVuNenChanDoanLoiTest {
    @Test
    void interruptedWorkerRestoresCurrentThreadInterruptStatus() {
        boolean previouslyInterrupted = Thread.interrupted();
        try {
            MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (previouslyInterrupted) Thread.currentThread().interrupt();
        }
    }
}

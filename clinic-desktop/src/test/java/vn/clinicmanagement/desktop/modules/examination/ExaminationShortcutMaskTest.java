package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExaminationShortcutMaskTest {
    @Test
    void headlessModeUsesControlMaskWithoutCallingPlatformProvider() {
        int mask = ExaminationPanel.resolveShortcutMask(true, () -> {
            throw new AssertionError("Headless mode must not query the platform mask");
        });

        assertEquals(java.awt.event.InputEvent.CTRL_DOWN_MASK, mask);
    }

    @Test
    void nonHeadlessModeUsesPlatformMask() {
        int syntheticPlatformMask = java.awt.event.InputEvent.ALT_DOWN_MASK;

        int mask = ExaminationPanel.resolveShortcutMask(false, () -> syntheticPlatformMask);

        assertEquals(syntheticPlatformMask, mask);
    }

    @Test
    void runtimeExceptionUsesControlMaskFallback() {
        int mask = ExaminationPanel.resolveShortcutMask(false, () -> {
            throw new IllegalStateException("Synthetic toolkit failure");
        });

        assertEquals(java.awt.event.InputEvent.CTRL_DOWN_MASK, mask);
    }

    @Test
    void errorFromPlatformProviderPropagatesUnchanged() {
        AssertionError syntheticFatalError = new AssertionError("Synthetic fatal provider error");

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> ExaminationPanel.resolveShortcutMask(false, () -> {
                    throw syntheticFatalError;
                }));

        assertSame(syntheticFatalError, thrown);
    }
}

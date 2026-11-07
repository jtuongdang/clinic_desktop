package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.Test;

import java.awt.event.InputEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExaminationShortcutMaskTest {

    @Test
    void headlessModeUsesControlMaskWithoutCallingPlatformProvider() {
        int shortcutMask = ExaminationPanel.resolveShortcutMask(true,
                () -> {
                    throw new AssertionError("platform provider should not be called");
                });

        assertEquals(InputEvent.CTRL_DOWN_MASK, shortcutMask);
    }

    @Test
    void nonHeadlessModeUsesPlatformMask() {
        int platformMask = InputEvent.META_DOWN_MASK;

        int shortcutMask = ExaminationPanel.resolveShortcutMask(false, () -> platformMask);

        assertEquals(platformMask, shortcutMask);
    }

    @Test
    void runtimeExceptionUsesControlMaskFallback() {
        int shortcutMask = ExaminationPanel.resolveShortcutMask(false,
                () -> {
                    throw new IllegalStateException("synthetic platform failure");
                });

        assertEquals(InputEvent.CTRL_DOWN_MASK, shortcutMask);
    }

    @Test
    void errorFromPlatformProviderPropagatesUnchanged() {
        AssertionError expectedFailure = new AssertionError("synthetic fatal platform failure");

        AssertionError actualFailure = assertThrows(AssertionError.class,
                () -> ExaminationPanel.resolveShortcutMask(false, () -> {
                    throw expectedFailure;
                }));

        assertSame(expectedFailure, actualFailure);
    }
}

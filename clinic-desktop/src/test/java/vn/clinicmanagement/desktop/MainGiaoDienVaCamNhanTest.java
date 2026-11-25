package vn.clinicmanagement.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainGiaoDienVaCamNhanTest {

    @Test
    void fallsBackWhenOptionalLookAndFeelIsMissing() {
        assertDoesNotThrow(() -> Main.configureLookAndFeel("missing.optional.LookAndFeel"));
    }

    @Test
    void rethrowsFatalErrorFromLookAndFeelSetup() {
        AssertionError expectedFailure = FatalLookAndFeel.FAILURE;

        AssertionError actualFailure = assertThrows(AssertionError.class,
                () -> Main.configureLookAndFeel(FatalLookAndFeel.class.getName()));

        assertSame(expectedFailure, actualFailure);
    }

    public static class FatalLookAndFeel {
        private static final AssertionError FAILURE = new AssertionError("synthetic setup failure");

        public static void setup() {
            throw FAILURE;
        }
    }
}

package vn.clinicmanagement.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainLookAndFeelTest {
    @Test
    void fallsBackWhenOptionalLookAndFeelIsMissing() {
        assertDoesNotThrow(() -> Main.configureLookAndFeel("missing.optional.LookAndFeel"));
    }

    @Test
    void rethrowsFatalErrorFromLookAndFeelSetup() {
        AssertionError failure = assertThrows(AssertionError.class,
                () -> Main.configureLookAndFeel(FatalLookAndFeel.class.getName()));

        org.junit.jupiter.api.Assertions.assertEquals("fatal setup failure", failure.getMessage());
    }

    public static final class FatalLookAndFeel {
        public static void setup() {
            throw new AssertionError("fatal setup failure");
        }
    }
}

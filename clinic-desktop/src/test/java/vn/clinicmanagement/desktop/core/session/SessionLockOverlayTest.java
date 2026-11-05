package vn.clinicmanagement.desktop.core.session;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SessionLockOverlayTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test void visibleGlassPaneBlocksWorkspaceInputAndOffersOnlyReauthenticationOrLogout() throws Exception {
        AtomicBoolean authenticated = new AtomicBoolean();
        AtomicBoolean loggedOut = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            SessionLockOverlay overlay = new SessionLockOverlay(credentials -> {
                authenticated.set(true);
                credentials.clear();
            }, () -> loggedOut.set(true));
            overlay.setSize(800, 600);
            overlay.setVisible(true);
            assertTrue(overlay.isInputBlocked());
            assertTrue(overlay.contains(400, 300));
            assertEquals(1, overlay.getComponentCount());
            overlay.getUnlockButton().doClick();
            assertTrue(authenticated.get());
            overlay.getLogoutButton().doClick();
            assertTrue(loggedOut.get());
            overlay.setVisible(false);
            assertFalse(overlay.isInputBlocked());
        });
    }
}

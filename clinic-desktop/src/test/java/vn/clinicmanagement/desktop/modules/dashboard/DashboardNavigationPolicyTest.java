package vn.clinicmanagement.desktop.modules.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.auth.*;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.RibbonButton;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.desktop.modules.home.HomeWelcomePanel;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import static org.junit.jupiter.api.Assertions.*;

class DashboardNavigationPolicyTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach void clear() { UserSession.getInstance().clearSession(); }

    @Test void allSensitiveRoutesStayDeniedEvenWithEveryKnownGrant() {
        var policy = new DashboardNavigationPolicy(Set.of("HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "EMR",
                "CLS", "PRESCRIPTION", "INVENTORY", "BILLING", "CATALOG", "SYSTEM", "ADMIN", "AUDIT_READ"));
        assertTrue(policy.canOpen("HOME"));
        for (String key : new String[]{"reception", "treatment", "doctor", "examination", "billing", "paraclinical",
                "pharmacy", "reports", "admin", "system", "catalog", "unknown", "PATIENT", "AUDIT_READ"}) {
            assertFalse(policy.canOpen(key), key);
        }
        assertFalse(policy.canOpen(null));
        assertFalse(new DashboardNavigationPolicy(Set.of()).canOpen("home"));
        assertFalse(new DashboardNavigationPolicy(Set.of("PATIENT")).canOpen("examination"));
        assertFalse(new DashboardNavigationPolicy(Set.of("PRESCRIPTION", "INVENTORY")).canOpen("pharmacy"));
    }

    @Test void directOpenRetainedTabAndImpersonatedHomePanelAreGuarded() throws Exception {
        // Preload the welcome image outside Swing, as the real login worker does.
        Class.forName(HomeWelcomePanel.class.getName());
        var lifecycle = lifecycle();
        SwingUtilities.invokeAndWait(() -> {
            CardLayout cards = new CardLayout();
            JPanel center = new JPanel(cards);
            JPanel tabs = new JPanel();
            var workspace = new MainDashboardFrame.GuardedWorkspace(center, cards, tabs, lifecycle,
                    new DashboardNavigationPolicy(Set.of("HOME", "EMR", "RECEPTION")));
            workspace.openView(new BaseViewPanel("HOME", "Impersonated", "") {
                @Override public void refreshData() { fail("No foreign panel should activate"); }
            });
            assertEquals(0, center.getComponentCount());
            HomeWelcomePanel home = new HomeWelcomePanel();
            workspace.openView(home);
            assertEquals(1, center.getComponentCount());
            assertEquals(1, tabs.getComponentCount());
            lifecycle.clear();
            workspace.openView(new HomeWelcomePanel());
            workspace.activateView("HOME");
            assertEquals(1, center.getComponentCount());
            workspace.disposeViews();
            assertEquals(0, center.getComponentCount());
            assertEquals(0, tabs.getComponentCount());
            workspace.openView(home);
            assertEquals(0, center.getComponentCount());
        });
    }

    @Test void dashboardHasNoFabricatedIdentityOrDisplayLabelConstructor() {
        var constructors = MainDashboardFrame.class.getConstructors();
        assertEquals(1, constructors.length);
        assertArrayEquals(new Class<?>[]{CompletedLoginSession.class, Set.class, SessionLifecycleService.class},
                constructors[0].getParameterTypes());
    }

    @Test void userDialogFormattingReadsVerifiedCompletedLoginSessionEvenIfSingletonCleared() {
        var loginSession = new CompletedLoginSession(701, "synthetic.701", "Synthetic employee",
                new AuthenticatedUser.RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new AuthenticatedUser.WorkScopeChoice(11, "Cơ sở tổng hợp", 21, "Khoa tổng hợp"),
                UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
        UserSession.getInstance().clearSession();

        assertEquals("Bác sĩ đang khám: Synthetic employee\n" +
                        "Cơ sở: Cơ sở tổng hợp\n" +
                        "Khoa phòng: Khoa tổng hợp\n" +
                        "Vai trò: Bác sĩ",
                MainDashboardFrame.formatDoctorUserInfo(loginSession));
        assertEquals("Thông tin người dùng: Synthetic employee\n" +
                        "Cơ sở: Cơ sở tổng hợp\n" +
                        "Khoa phòng: Khoa tổng hợp\n" +
                        "Vai trò: Bác sĩ",
                MainDashboardFrame.formatStaffUserInfo(loginSession));
    }

    @Test void ribbonButtonsDisableOnIdleLockAndReenableOnlyAllowedRoutesAfterReauthentication() throws Exception {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var role = new AuthenticatedUser.RoleChoice(42, "DOCTOR", "Bác sĩ");
        var scope = new AuthenticatedUser.WorkScopeChoice(11, "Cơ sở tổng hợp", 21, "Khoa tổng hợp");
        var lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)), clock,
                new AuditWritePort() {
                    @Override public void appendInCurrentTransaction(SystemAuditEntry entry) { fail("Unexpected transactional audit"); }
                    @Override public void appendIndependently(SystemAuditEntry entry) {}
                    @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) { fail("Unexpected medical audit"); }
                    @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) { fail("Unexpected medical audit"); }
                }, Runnable::run);
        lifecycle.startSession(new CompletedLoginSession(701, "synthetic.701", "Synthetic employee",
                role, scope, UUID.randomUUID(), clock.instant()));
        var navigation = new DashboardNavigationPolicy(Set.of("HOME"));
        Map<String, RibbonButton> ribbonButtons = Map.of(
                "home", new RibbonButton("Trang Chủ", "", "home", null),
                "reception", new RibbonButton("Đón Tiếp", "", "reception", null),
                "examination", new RibbonButton("Khám Bệnh", "", "examination", null),
                "user", new RibbonButton("Người Dùng", "", "user", null),
                "exit", new RibbonButton("Kết Thúc", "", "exit", null));

        MainDashboardFrame.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("home", "user", "exit"));

        clock.advance(Duration.ofMinutes(15));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        MainDashboardFrame.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("exit"));

        var authenticated = new AuthenticatedUser(701, "synthetic.701", "Synthetic employee",
                java.util.List.of(role), java.util.List.of(scope));
        FutureTask<Boolean> reauthentication = new FutureTask<>(() -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return lifecycle.reauthenticate(authenticated);
        });
        Thread worker = new Thread(reauthentication);
        worker.start();
        assertTrue(reauthentication.get());
        MainDashboardFrame.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("home", "user", "exit"));
    }

    @Test void lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var lockOverlay = new SessionLockOverlay(credentials -> {}, () -> {});

            MainDashboardFrame.syncLockOverlayForSessionState(SessionLifecycleService.State.ACTIVE, lockOverlay);
            assertFalse(lockOverlay.isVisible());
            assertFalse(lockOverlay.isInputBlocked());

            MainDashboardFrame.syncLockOverlayForSessionState(SessionLifecycleService.State.LOCKED_IDLE, lockOverlay);
            assertTrue(lockOverlay.isVisible());
            assertTrue(lockOverlay.isInputBlocked());

            MainDashboardFrame.syncLockOverlayForSessionState(SessionLifecycleService.State.ACTIVE, lockOverlay);
            assertFalse(lockOverlay.isVisible());
            assertFalse(lockOverlay.isInputBlocked());

            MainDashboardFrame.syncLockOverlayForSessionState(SessionLifecycleService.State.LOCKED_ABSOLUTE, lockOverlay);
            assertTrue(lockOverlay.isVisible());
            assertTrue(lockOverlay.isInputBlocked());
        });
    }

    private void assertRibbonButtons(Map<String, RibbonButton> ribbonButtons, Set<String> enabledKeys) {
        for (var entry : ribbonButtons.entrySet()) {
            assertEquals(enabledKeys.contains(entry.getKey()), entry.getValue().isEnabled(), entry.getKey());
        }
    }

    private SessionLifecycleService lifecycle() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        var lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)), Clock.fixed(now, ZoneOffset.UTC),
                new AuditWritePort() {
                    @Override public void appendInCurrentTransaction(SystemAuditEntry entry) { fail("No transactional session audit"); }
                    @Override public void appendIndependently(SystemAuditEntry entry) {}
                    @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) { fail("No medical audit"); }
                    @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) { fail("No medical audit"); }
                });
        lifecycle.startSession(new CompletedLoginSession(701, "synthetic.701", "Synthetic employee",
                new AuthenticatedUser.RoleChoice(42, "DOCTOR", "Synthetic doctor"),
                new AuthenticatedUser.WorkScopeChoice(11, "Synthetic facility", 21, "Synthetic department"), UUID.randomUUID(), now));
        return lifecycle;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}

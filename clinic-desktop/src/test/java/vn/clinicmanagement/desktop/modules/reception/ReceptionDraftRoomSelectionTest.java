package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.ReceptionQueueSnapshot;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.reception.components.MedicalPopupChooser;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.domain.facility.PhongKham;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic headless UI fixtures only; no JDBC composition or external services. */
class ReceptionDraftRoomSelectionTest {
    private static final RoleChoice ROLE = new RoleChoice(42, "RECEPTIONIST", "Synthetic Reception");
    private static final WorkScopeChoice SCOPE = new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department");
    private final MutableClock clock = new MutableClock();
    private final AtomicReference<List<PhongKham>> activeRooms = new AtomicReference<>(rooms());
    private SessionLifecycleService lifecycle;
    private ReceptionPanel panel;

    @BeforeEach
    void showDraftWithNonDefaultRoom() throws Exception {
        UserSession.getInstance().clearSession();
        lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(5), Duration.ofHours(1)), clock);
        replace(701, ROLE, SCOPE);
        edt(() -> {
            panel = new ReceptionPanel(lifecycle, new ReceptionCompositionRoot.Services(null,
                    command -> { throw new AssertionError("No registration writes expected"); },
                    query -> Result.success(List.of()),
                    query -> Result.success(new ReceptionQueueSnapshot(List.of(), List.of())),
                    query -> {
                        assertFalse(SwingUtilities.isEventDispatchThread());
                        return Result.success(activeRooms.get());
                    })) {
                @Override void showMessage(String message, String title, int type) {
                    fail("Unexpected dialog: " + message);
                }
            };
            panel.onShow();
        });
        awaitLoad();
        edt(() -> {
            assertEquals(31, form().selectedClinicRoomId(), "Fresh drafts retain the existing initial default");
            chooser().setText("Synthetic Z Room");
            assertRoom(32, "Synthetic Z Room");
            ((JTextField) find(panel, "fullName")).setText("Synthetic unsaved draft");
        });
    }

    @AfterEach
    void cleanUp() {
        if (panel != null) edt(panel::onHide);
        lifecycle.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hide", "idle", "absolute"})
    void nonDefaultActiveRoomSurvivesSameContextInvalidation(String transition) throws Exception {
        resumeAfter(transition);
        awaitLoad();
        edt(() -> {
            assertRoom(32, "Synthetic Z Room");
            assertEquals("Synthetic unsaved draft", ((JTextField) find(panel, "fullName")).getText());
        });
        // A second protected-read refresh must preserve the current choice too.
        edt(panel::refreshData);
        awaitLoad();
        edt(() -> assertRoom(32, "Synthetic Z Room"));
    }

    @ParameterizedTest
    @CsvSource({"hide,false", "idle,false", "absolute,false", "hide,true", "idle,true", "absolute,true"})
    void removedRoomLeavesSelectionEmptyUntilUserChoosesAgain(String transition, boolean noRooms) throws Exception {
        activeRooms.set(noRooms ? List.of() : List.of(room(31, "Synthetic A Room")));
        resumeAfter(transition);
        awaitLoad();
        edt(() -> assertRoom(0, ""));
        // Even if the old room returns later, the unavailable selection must remain empty.
        activeRooms.set(rooms());
        edt(panel::refreshData);
        awaitLoad();
        edt(() -> {
            assertRoom(0, "");
            chooser().setText("Synthetic A Room");
        });
        resumeAfter("hide");
        awaitLoad();
        edt(() -> assertRoom(31, "Synthetic A Room"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"identity", "roleId", "roleCode", "facility", "department", "uuid", "logout", "hiddenLogout"})
    void replacementOrLogoutDoesNotReusePreviousSessionRoom(String change) throws Exception {
        edt(() -> {
            if (change.equals("hiddenLogout")) panel.onHide();
            if (change.equals("logout") || change.equals("hiddenLogout")) {
                lifecycle.logout();
                if (change.equals("logout")) assertRoom(0, "");
            }
            replace(change.equals("identity") ? 702 : 701,
                    change.equals("roleId") ? new RoleChoice(43, "RECEPTIONIST", "Synthetic Other Role")
                            : change.equals("roleCode") ? new RoleChoice(42, "DOCTOR", "Synthetic Doctor") : ROLE,
                    change.equals("facility") ? new WorkScopeChoice(12, "Synthetic Other Facility", 21, "Synthetic Department")
                            : change.equals("department") ? new WorkScopeChoice(11, "Synthetic Facility", 22, "Synthetic Other Department") : SCOPE);
            if (change.equals("hiddenLogout")) panel.onShow();
            assertRoom(0, "");
        });
        awaitLoad();
        // Both IDs are deliberately still returned: clearing must follow session ownership, not catalog membership.
        edt(() -> {
            assertRoom(31, "Synthetic A Room");
            assertEquals("", ((JTextField) find(panel, "fullName")).getText());
        });
    }

    @Test
    void roomIsRestoredByIdAfterRenameRatherThanByOldLabel() throws Exception {
        activeRooms.set(List.of(room(33, "Synthetic Z Room"), room(32, "Synthetic Renamed Room"),
                room(31, "Synthetic A Room")));
        resumeAfter("hide");
        awaitLoad();
        edt(() -> assertRoom(32, "Synthetic Renamed Room"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void queuedLockCleanupCannotLoseRoomWhenReauthenticationOvertakesIt(boolean absolute) throws Exception {
        edt(() -> {
            Thread callback = new Thread(() -> {
                clock.advance(absolute ? Duration.ofHours(1) : Duration.ofMinutes(5));
                lifecycle.evaluate();
            }, "synthetic-session-lock");
            callback.start();
            try { callback.join(2000); }
            catch (InterruptedException exception) { throw new AssertionError(exception); }
            assertFalse(callback.isAlive());
            assertTrue(lifecycle.reauthenticate(user()));
        });
        awaitLoad();
        edt(() -> assertRoom(32, "Synthetic Z Room"));
    }

    private void resumeAfter(String transition) {
        edt(() -> {
            if (transition.equals("hide")) {
                panel.onHide();
                panel.onHide(); // Repeated clearing must not overwrite the retained ID with an empty selection.
                assertRoom(0, "");
                panel.onShow();
            } else {
                UUID previous = lifecycle.currentAuthorizationContext().orElseThrow().sessionId();
                boolean absolute = transition.equals("absolute");
                clock.advance(absolute ? Duration.ofHours(1) : Duration.ofMinutes(5));
                assertEquals(absolute ? SessionLifecycleService.State.LOCKED_ABSOLUTE : SessionLifecycleService.State.LOCKED_IDLE,
                        lifecycle.evaluate());
                assertRoom(0, "");
                assertTrue(lifecycle.reauthenticate(user()));
                UUID current = lifecycle.currentAuthorizationContext().orElseThrow().sessionId();
                if (absolute) assertNotEquals(previous, current);
                else assertEquals(previous, current);
            }
        });
    }

    private void replace(int employee, RoleChoice role, WorkScopeChoice scope) {
        lifecycle.startSession(employee, "synthetic", "Synthetic Reception", role, scope, UUID.randomUUID().toString());
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(701, "synthetic", "Synthetic Reception", List.of(ROLE), List.of(SCOPE));
    }

    private void assertRoom(int id, String name) {
        assertTrue(SwingUtilities.isEventDispatchThread());
        assertEquals(id, form().selectedClinicRoomId());
        assertEquals(name, form().selectedClinicRoomName());
    }

    private PatientRegistrationFormPanel form() { return (PatientRegistrationFormPanel) read(panel, "formPanel"); }
    private MedicalPopupChooser chooser() { return (MedicalPopupChooser) find(panel, "clinicRoom"); }

    private void awaitLoad() throws Exception {
        await(() -> read(panel, "loadWorker") == null);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        AtomicBoolean satisfied = new AtomicBoolean();
        do {
            edt(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) return;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        fail("Timed out waiting for synthetic room load");
    }

    private static Object read(Object target, String name) {
        try {
            Field field = ReceptionPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static Component find(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container child) {
                Component found = find(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void edt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) { action.run(); return; }
        try { SwingUtilities.invokeAndWait(action); }
        catch (Exception exception) { throw new AssertionError(exception); }
    }

    private static List<PhongKham> rooms() {
        return List.of(room(32, "Synthetic Z Room"), room(31, "Synthetic A Room"));
    }

    private static PhongKham room(int id, String name) {
        PhongKham room = new PhongKham();
        room.setId(id); room.setMaKhoa(21); room.setTenPhong(name); room.setHoatDong(true);
        return room;
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        synchronized void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public synchronized Instant instant() { return now; }
    }
}

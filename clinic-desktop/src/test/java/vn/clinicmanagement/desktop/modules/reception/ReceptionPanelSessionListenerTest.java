package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BenhNhan;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic fixtures only: no JDBC composition, external services, or display server. */
class ReceptionPanelSessionListenerTest {
    private static final RoleChoice ROLE = new RoleChoice(42, "RECEPTIONIST", "Synthetic Reception");
    private static final WorkScopeChoice SCOPE = new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department");
    private final MutableClock clock = new MutableClock();
    private final List<CountDownLatch> gates = new ArrayList<>();
    private SessionLifecycleService lifecycle;
    private TestPanel panel;

    @BeforeEach
    void startSession() {
        UserSession.getInstance().clearSession();
        lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(5), Duration.ofHours(1)), clock);
        replace(701, SCOPE, ROLE);
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (CountDownLatch gate : gates) gate.countDown();
        if (panel != null) edt(panel::onHide);
        lifecycle.clear();
    }

    @Test
    void loadsProtectedDataOffEdtWithCapturedPerOperationContextsAndMutatesOnlyEdt() throws Exception {
        AtomicReference<GetActiveReceptionRoomsQuery> roomsQuery = new AtomicReference<>();
        AtomicReference<GetReceptionQueueQuery> queueQuery = new AtomicReference<>();
        List<SearchPatientsQuery> searches = new java.util.concurrent.CopyOnWriteArrayList<>();
        panel = create(command -> Result.success(receipt()), query -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            searches.add(query);
            return Result.success(List.of(patient()));
        }, query -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            queueQuery.set(query);
            return Result.success(snapshot());
        }, query -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            roomsQuery.set(query);
            return Result.success(List.of(room()));
        });
        AtomicBoolean offEdt = watchMutations();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("searchPhone").setText("synthetic-phone");
            field("searchCccd").setText("synthetic-id");
            form().triggerSearch();
        });
        await(() -> field("patientCode").getText().equals("SYN-501"));
        AuthorizationContext expected = lifecycle.currentAuthorizationContext().orElseThrow();
        assertEquals(expected, roomsQuery.get().authorizationContext());
        assertEquals(expected, queueQuery.get().authorizationContext());
        assertNotEquals(roomsQuery.get().auditActorContext().correlationId(), queueQuery.get().auditActorContext().correlationId());
        assertEquals(2, searches.size());
        for (SearchPatientsQuery query : searches) {
            assertEquals(expected, query.authorizationContext());
            assertEquals(701, query.auditActorContext().employeeId());
            assertEquals(11, query.auditActorContext().facilityId());
            assertEquals(21, query.auditActorContext().departmentId());
        }
        assertNotEquals(searches.get(0).auditActorContext().correlationId(), searches.get(1).auditActorContext().correlationId());
        lifecycle.logout();
        edt(() -> assertCleared(true));
        assertFalse(offEdt.get());
        assertEquals(0, panel.dialogs.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void callbackCancelsRoomOrQueueAndSearchWhileEdtIsBlocked(boolean blockRooms) throws Exception {
        CountDownLatch readEntered = new CountDownLatch(1);
        CountDownLatch searchEntered = new CountDownLatch(1);
        CountDownLatch readInterrupted = new CountDownLatch(1);
        CountDownLatch searchInterrupted = new CountDownLatch(1);
        CountDownLatch release = gate();
        AtomicInteger queueReads = new AtomicInteger();
        panel = create(command -> Result.success(receipt()), query -> {
            searchEntered.countDown();
            holdIgnoringInterrupt(release, searchInterrupted);
            return Result.success(List.of(patient()));
        }, query -> {
            queueReads.incrementAndGet();
            if (!blockRooms) {
                readEntered.countDown();
                holdIgnoringInterrupt(release, readInterrupted);
            }
            return Result.success(snapshot());
        }, query -> {
            if (blockRooms) {
                readEntered.countDown();
                holdIgnoringInterrupt(release, readInterrupted);
            }
            return Result.success(List.of(room()));
        });
        edt(() -> {
            panel.onShow();
            field("fullName").setText("Synthetic unsaved draft");
            field("searchPhone").setText("synthetic-search");
            form().triggerSearch();
        });
        assertTrue(readEntered.await(2, TimeUnit.SECONDS));
        assertTrue(searchEntered.await(2, TimeUnit.SECONDS));
        SwingWorker<?, ?> load = worker("loadWorker");
        SwingWorker<?, ?> search = worker("searchWorker");
        long before = generation();
        edt(() -> {
            // Cleanup cannot run on the EDT until this turn exits; cancellation must already have happened.
            Thread callback = new Thread(lifecycle::logout, "synthetic-session-change");
            callback.start();
            join(callback);
            assertTrue(load.isCancelled());
            assertTrue(search.isCancelled());
            assertTrue(generation() > before);
            assertEquals("Synthetic unsaved draft", field("fullName").getText());
        });
        assertTrue(readInterrupted.await(2, TimeUnit.SECONDS));
        assertTrue(searchInterrupted.await(2, TimeUnit.SECONDS));
        edt(() -> assertCleared(true));
        release.countDown();
        drainCallbacks();
        edt(() -> assertCleared(true));
        if (blockRooms) assertEquals(0, queueReads.get(), "Cancellation must prevent the next protected read");
        assertEquals(0, panel.dialogs.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void completedReadCallbacksCannotRenderResultsOrErrorsAfterLogout(boolean success) throws Exception {
        panel = create(command -> Result.success(receipt()), query -> {
            if (!success) throw new IllegalStateException("Synthetic search failure");
            return Result.success(List.of(patient()));
        }, query -> {
            if (!success) throw new IllegalStateException("Synthetic queue failure");
            return Result.success(snapshot());
        }, query -> Result.success(List.of(room())));
        AtomicInteger rowsAdded = new AtomicInteger();
        edt(() -> tables(panel).forEach(table -> table.getModel().addTableModelListener(event -> {
            if (table.getRowCount() > 0) rowsAdded.incrementAndGet();
        })));
        edt(() -> {
            panel.onShow();
            field("searchPhone").setText("synthetic-search");
            form().triggerSearch();
            finishBackground(worker("loadWorker"));
            finishBackground(worker("searchWorker"));
            // Both Futures are already complete, but done() is still waiting for this EDT turn.
            lifecycle.logout();
        });
        drainCallbacks();
        edt(() -> assertCleared(true));
        assertEquals(0, rowsAdded.get());
        assertEquals(0, panel.dialogs.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void idleAndAbsoluteLocksPreserveDraftThroughReauthentication(boolean absolute) throws Exception {
        panel = defaults();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic unsaved draft");
            field("reason").setText("Synthetic draft reason");
            field("insuranceNumber").setText("SYNTHETIC-CARD");
        });
        UUID originalId = lifecycle.currentAuthorizationContext().orElseThrow().sessionId();
        clock.advance(absolute ? Duration.ofHours(1) : Duration.ofMinutes(5));
        assertEquals(absolute ? SessionLifecycleService.State.LOCKED_ABSOLUTE : SessionLifecycleService.State.LOCKED_IDLE,
                lifecycle.evaluate());
        edt(() -> {
            assertCleared(false);
            assertEquals("Synthetic unsaved draft", field("fullName").getText());
            assertEquals("SYNTHETIC-CARD", field("insuranceNumber").getText());
        });
        assertTrue(lifecycle.reauthenticate(new AuthenticatedUser(701, "synthetic", "Synthetic Reception",
                List.of(ROLE), List.of(SCOPE))));
        await(() -> queueRows() == 1);
        edt(() -> {
            assertEquals("Synthetic unsaved draft", field("fullName").getText());
            assertEquals("Synthetic draft reason", field("reason").getText());
            assertEquals("SYNTHETIC-CARD", field("insuranceNumber").getText());
        });
        if (absolute) assertNotEquals(originalId, lifecycle.currentAuthorizationContext().orElseThrow().sessionId());
        assertEquals(0, panel.dialogs.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"identity", "scope", "role", "uuid"})
    void validReplacementClearsPriorSessionFormAndSearchBeforeReload(String change) throws Exception {
        CountDownLatch release = gate();
        CountDownLatch reloadEntered = new CountDownLatch(1);
        AuthorizationContext original = lifecycle.currentAuthorizationContext().orElseThrow();
        panel = create(command -> Result.success(receipt()), query -> Result.success(List.of(patient())),
                query -> Result.success(snapshot()), query -> {
                    if (!query.authorizationContext().equals(original)) {
                        reloadEntered.countDown();
                        holdIgnoringInterrupt(release, new CountDownLatch(1));
                    }
                    return Result.success(List.of(room()));
                });
        AtomicBoolean offEdt = watchMutations();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            form().selectPatient(patient().patient(), null);
            field("fullName").setText("Synthetic previous draft");
            field("searchPhone").setText("synthetic-search");
            field("searchCccd").setText("synthetic-id");
        });
        replace(change.equals("identity") ? 702 : 701,
                change.equals("scope") ? new WorkScopeChoice(12, "Synthetic Other Facility", 22, "Synthetic Other Department") : SCOPE,
                change.equals("role") ? new RoleChoice(43, "RECEPTIONIST", "Synthetic Other Role") : ROLE);
        edt(() -> assertCleared(true));
        assertTrue(reloadEntered.await(2, TimeUnit.SECONDS));
        release.countDown();
        await(() -> queueRows() == 1);
        edt(() -> assertEquals("", field("patientCode").getText()));
        assertFalse(offEdt.get());
    }

    @Test
    void logoutThenReplacementBeforeEdtCleanupStillClearsDraft() throws Exception {
        panel = defaults();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic previous draft");
            Thread callback = new Thread(() -> {
                lifecycle.logout();
                replace(701, SCOPE, ROLE);
            });
            callback.start();
            join(callback);
        });
        await(() -> queueRows() == 1);
        edt(() -> assertEquals("", field("fullName").getText()));
    }

    @Test
    void queuedLogoutCleanupCannotEraseDraftEnteredAfterNewerEdtReplacement() throws Exception {
        panel = defaults();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic previous draft");
            Thread callback = new Thread(lifecycle::logout);
            callback.start();
            join(callback);
            replace(701, SCOPE, ROLE);
            assertEquals("", field("fullName").getText());
            field("fullName").setText("Synthetic new session draft");
        });
        await(() -> queueRows() == 1);
        edt(() -> assertEquals("Synthetic new session draft", field("fullName").getText()));
    }

    @Test
    void absoluteUnlockBeforeQueuedLockCleanupPreservesDraft() throws Exception {
        panel = defaults();
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic unsaved draft");
            Thread callback = new Thread(() -> {
                clock.advance(Duration.ofHours(1));
                lifecycle.evaluate();
            });
            callback.start();
            join(callback);
            assertTrue(lifecycle.reauthenticate(new AuthenticatedUser(701, "synthetic", "Synthetic Reception",
                    List.of(ROLE), List.of(SCOPE))));
        });
        await(() -> queueRows() == 1);
        edt(() -> assertEquals("Synthetic unsaved draft", field("fullName").getText()));
    }

    @Test
    void modalSearchSelectionIsRejectedWhenSessionChangesInsideDialog() throws Exception {
        panel = defaults();
        panel.beforeChoice = lifecycle::logout;
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic unsaved draft");
            field("searchPhone").setText("synthetic-search");
            form().triggerSearch();
        });
        await(() -> panel.choices.get() == 1);
        edt(() -> assertCleared(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"lock", "logout", "replacement", "hide"})
    void registrationTransactionIsNeverInterruptedAndStaleCompletionCannotClearDraftOrShowReceipt(String transition) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = gate();
        AtomicBoolean interrupted = new AtomicBoolean();
        panel = create(command -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            entered.countDown();
            try { assertTrue(release.await(3, TimeUnit.SECONDS)); }
            catch (InterruptedException exception) { interrupted.set(true); }
            return Result.success(receipt());
        }, query -> Result.success(List.of(patient())), query -> Result.success(snapshot()),
                query -> Result.success(List.of(room())));
        edt(panel::onShow);
        await(() -> queueRows() == 1);
        edt(() -> {
            field("fullName").setText("Synthetic Patient");
            field("dateOfBirth").setText("01/01/2000");
            invokeSave();
        });
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        SwingWorker<?, ?> registration = worker("registrationWorker");
        edt(() -> {
            switch (transition) {
                case "lock" -> { clock.advance(Duration.ofMinutes(5)); lifecycle.evaluate(); }
                case "logout" -> lifecycle.logout();
                case "replacement" -> replace(702, SCOPE, ROLE);
                case "hide" -> panel.onHide();
                default -> fail("Unknown transition");
            }
            if (transition.equals("replacement")) field("fullName").setText("Synthetic new session draft");
            assertFalse(registration.isCancelled());
        });
        release.countDown();
        registration.get(2, TimeUnit.SECONDS);
        await(() -> worker("registrationWorker") == null);
        assertFalse(interrupted.get());
        assertEquals(0, panel.dialogs.get());
        edt(() -> assertEquals(switch (transition) {
            case "lock", "hide" -> "Synthetic Patient";
            case "replacement" -> "Synthetic new session draft";
            default -> "";
        }, field("fullName").getText()));
    }

    @Test
    void showHideRegistersExactlyOnceRemovesListenerAndRejectsPendingReads() throws Exception {
        panel = defaults();
        edt(() -> {
            assertEquals(0, listenerCount());
            panel.onShow();
            panel.onShow();
            assertEquals(1, listenerCount());
            panel.onHide();
            panel.onHide();
            assertEquals(0, listenerCount());
            assertCleared(false);
        });
        drainCallbacks();
        edt(() -> assertCleared(false));
        edt(() -> {
            field("fullName").setText("Synthetic hidden draft");
            lifecycle.logout();
            replace(702, SCOPE, ROLE);
            panel.onShow();
            assertEquals(1, listenerCount());
            assertEquals("", field("fullName").getText());
        });
        await(() -> queueRows() == 1);
    }

    private TestPanel defaults() {
        return create(command -> Result.success(receipt()), query -> Result.success(List.of(patient())),
                query -> Result.success(snapshot()), query -> Result.success(List.of(room())));
    }

    private TestPanel create(CommandHandler<RegisterPatientCommand, RegistrationReceipt> register,
                             QueryHandler<SearchPatientsQuery, List<PatientSearchItem>> search,
                             QueryHandler<GetReceptionQueueQuery, ReceptionQueueSnapshot> queue,
                             QueryHandler<GetActiveReceptionRoomsQuery, List<PhongKham>> rooms) {
        AtomicReference<TestPanel> created = new AtomicReference<>();
        edt(() -> created.set(new TestPanel(lifecycle, new ReceptionCompositionRoot.Services(null, register, search, queue, rooms))));
        return created.get();
    }

    private AtomicBoolean watchMutations() {
        AtomicBoolean offEdt = new AtomicBoolean();
        edt(() -> {
            tables(panel).forEach(table -> table.getModel().addTableModelListener(event -> {
                if (!SwingUtilities.isEventDispatchThread()) offEdt.set(true);
            }));
            field("fullName").getDocument().addDocumentListener(new DocumentListener() {
                private void changed() { if (!SwingUtilities.isEventDispatchThread()) offEdt.set(true); }
                @Override public void insertUpdate(DocumentEvent event) { changed(); }
                @Override public void removeUpdate(DocumentEvent event) { changed(); }
                @Override public void changedUpdate(DocumentEvent event) { changed(); }
            });
        });
        return offEdt;
    }

    private void replace(int actor, WorkScopeChoice scope, RoleChoice role) {
        lifecycle.startSession(actor, "synthetic", "Synthetic Reception", role, scope, UUID.randomUUID().toString());
    }

    private void assertCleared(boolean draft) {
        assertTrue(SwingUtilities.isEventDispatchThread());
        for (JTable table : tables(panel)) assertEquals(0, table.getRowCount());
        assertEquals(0, form().selectedClinicRoomId());
        assertEquals("", form().selectedClinicRoomName());
        assertNull(read(read(panel, "topToolbar"), "nextQueueMessage"));
        if (draft) {
            assertFalse(form().hasUnsavedInput());
            for (String name : List.of("patientCode", "fullName", "searchPhone", "searchCccd", "insuranceNumber"))
                assertEquals("", field(name).getText(), name);
        }
    }

    private PatientRegistrationFormPanel form() { return (PatientRegistrationFormPanel) read(panel, "formPanel"); }
    private JTextField field(String name) { return (JTextField) find(panel, name); }
    private int queueRows() { return tables(panel).get(0).getRowCount(); }
    private SwingWorker<?, ?> worker(String name) { return (SwingWorker<?, ?>) read(panel, name); }
    private long generation() { return ((AtomicLong) read(panel, "sessionGeneration")).get(); }
    private int listenerCount() { return ((List<?>) read(lifecycle, "listeners")).size(); }

    private void invokeSave() {
        try {
            var method = ReceptionPanel.class.getDeclaredMethod("saveRegistration");
            method.setAccessible(true);
            method.invoke(panel);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static Object read(Object target, String name) {
        try {
            Class<?> type = target instanceof ReceptionPanel ? ReceptionPanel.class : target.getClass();
            Field field = type.getDeclaredField(name);
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

    private static List<JTable> tables(Container root) {
        List<JTable> tables = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (component instanceof JTable table) tables.add(table);
            else if (component instanceof Container child) tables.addAll(tables(child));
        }
        return tables;
    }

    private CountDownLatch gate() {
        CountDownLatch gate = new CountDownLatch(1);
        gates.add(gate);
        return gate;
    }

    private static void holdIgnoringInterrupt(CountDownLatch release, CountDownLatch interrupted) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (release.getCount() > 0 && System.nanoTime() < deadline) {
            try { release.await(100, TimeUnit.MILLISECONDS); }
            catch (InterruptedException exception) { interrupted.countDown(); }
        }
        assertEquals(0, release.getCount(), "Synthetic read was not released");
    }

    private static void finishBackground(SwingWorker<?, ?> worker) {
        assertNotNull(worker);
        try { worker.get(2, TimeUnit.SECONDS); }
        catch (java.util.concurrent.ExecutionException expectedSyntheticFailure) { /* done() must reject this too. */ }
        catch (Exception exception) { throw new AssertionError(exception); }
    }

    private static void join(Thread thread) {
        try { thread.join(2000); }
        catch (InterruptedException exception) { throw new AssertionError(exception); }
        assertFalse(thread.isAlive(), "Session callback must not wait for EDT cleanup");
    }

    private static void edt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) { action.run(); return; }
        try { SwingUtilities.invokeAndWait(action); }
        catch (Exception exception) { throw new AssertionError(exception); }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        AtomicBoolean satisfied = new AtomicBoolean();
        do {
            edt(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) return;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        fail("Timed out waiting for synthetic worker completion");
    }

    private static void drainCallbacks() throws Exception {
        // SwingWorker batches done() callbacks on a short Swing timer.
        CountDownLatch drained = new CountDownLatch(1);
        edt(() -> {
            Timer timer = new Timer(100, event -> drained.countDown());
            timer.setRepeats(false);
            timer.start();
        });
        assertTrue(drained.await(2, TimeUnit.SECONDS));
        edt(() -> {});
    }

    private static PhongKham room() {
        PhongKham room = new PhongKham();
        room.setId(31); room.setMaKhoa(21); room.setTenPhong("Synthetic Room");
        return room;
    }

    private static PatientSearchItem patient() {
        BenhNhan patient = new BenhNhan();
        patient.setId(501); patient.setHo("Synthetic"); patient.setTen("Patient");
        patient.setMaBenhNhanFormat("SYN-501"); patient.setNgaySinh(LocalDate.of(2000, 1, 1));
        return new PatientSearchItem(patient, null);
    }

    private static ReceptionQueueSnapshot snapshot() {
        return new ReceptionQueueSnapshot(List.of(new ReceptionQueueItem(101, 501, "SYN-501", "Synthetic Patient",
                31, "Synthetic Room", 1, "SYN-001", "CHO_KHAM", LocalDateTime.of(2026, 1, 1, 8, 0))),
                List.of(new ClinicRoomQueueSummary(31, "Synthetic Room", "Synthetic Department", 1, 1, 0)));
    }

    private static RegistrationReceipt receipt() { return new RegistrationReceipt(501, "SYN-501", 101, 1, "SYN-001"); }

    private static final class TestPanel extends ReceptionPanel {
        final AtomicInteger dialogs = new AtomicInteger();
        final AtomicInteger choices = new AtomicInteger();
        Runnable beforeChoice = () -> {};
        TestPanel(SessionLifecycleService lifecycle, ReceptionCompositionRoot.Services services) { super(lifecycle, services); }
        @Override void showMessage(String message, String title, int type) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            dialogs.incrementAndGet();
        }
        @Override PatientSearchItem choosePatient(List<PatientSearchItem> matches) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            beforeChoice.run();
            choices.incrementAndGet();
            return matches.get(0);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        synchronized void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public synchronized Instant instant() { return now; }
    }
}

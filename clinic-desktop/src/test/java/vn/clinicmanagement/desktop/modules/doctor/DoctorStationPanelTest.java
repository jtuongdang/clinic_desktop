package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.patienthistory.GetPatientHistoryQuery;
import vn.clinicmanagement.application.reception.GetDoctorQueueQuery;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;

import javax.swing.*;
import java.awt.*;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class DoctorStationPanelTest {
    private SessionLifecycleService lifecycle;
    private DoctorStationPanel panel;

    @AfterEach
    void cleanUp() throws Exception {
        if (panel != null) SwingUtilities.invokeAndWait(panel::onHide);
        if (lifecycle != null) lifecycle.clear();
    }

    @Test
    void loadsTeamQueueAndQueriesHistoryForSelectedPatientOffEdt() throws Exception {
        lifecycle = activeLifecycle();
        ReceptionQueueItem first = queueItem(101, 501, "SYN-501");
        ReceptionQueueItem second = queueItem(102, 502, "SYN-502");
        AtomicReference<GetDoctorQueueQuery> queueQuery = new AtomicReference<>();
        AtomicReference<GetPatientHistoryQuery> historyQuery = new AtomicReference<>();
        AtomicBoolean queueRanOffEdt = new AtomicBoolean();
        AtomicBoolean historyRanOffEdt = new AtomicBoolean();
        panel = new DoctorStationPanel(lifecycle, query -> {
            queueQuery.set(query);
            queueRanOffEdt.set(!SwingUtilities.isEventDispatchThread());
            return Result.success(List.of(first, second));
        }, query -> {
            historyQuery.set(query);
            historyRanOffEdt.set(!SwingUtilities.isEventDispatchThread());
            return Result.success(List.of(history(query.patientId(), "Synthetic approved history")));
        });

        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 2);

        GetDoctorQueueQuery loadedQuery = queueQuery.get();
        assertNotNull(loadedQuery);
        assertEquals(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), loadedQuery.date());
        assertEquals(11, loadedQuery.authorizationContext().facilityId());
        assertEquals(21, loadedQuery.authorizationContext().departmentId());
        assertTrue(queueRanOffEdt.get());
        JTable queueTable = queueTable();
        ReceptionQueueItem queued = (ReceptionQueueItem) queueTable.getModel().getValueAt(0, 0);
        assertEquals(101, queued.visitId());
        assertEquals(501, queued.patientId());

        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(0, 0));
        await(() -> historyRowCount() == 1);
        assertEquals(501, historyQuery.get().patientId());
        assertTrue(historyRanOffEdt.get());
        assertEquals("Synthetic approved history", historyTable().getValueAt(0, 1));
        assertFalse(historyTable().getModel().isCellEditable(0, 1));

        SwingUtilities.invokeAndWait(queueTable::clearSelection);
        await(() -> historyRowCount() == 0);
    }

    @Test
    void denialAndAuditFailureClearAnyPreviouslyDisplayedHistory() throws Exception {
        lifecycle = activeLifecycle();
        AtomicInteger historyReads = new AtomicInteger();
        panel = new DoctorStationPanel(lifecycle,
                query -> Result.success(List.of(queueItem(101, 501, "SYN-501"),
                        queueItem(102, 502, "SYN-502"), queueItem(103, 503, "SYN-503"))),
                query -> {
                    historyReads.incrementAndGet();
                    if (query.patientId() == 501) {
                        return Result.success(List.of(history(501, "Previously displayed synthetic detail")));
                    }
                    return query.patientId() == 502
                            ? Result.failure("AUTHORIZATION_DENIED", "denied")
                            : Result.failure("PATIENT_HISTORY_AUDIT_FAILED", "audit failed");
                });
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 3);

        JTable queueTable = queueTable();
        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(0, 0));
        await(() -> historyRowCount() == 1);

        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(1, 1));
        await(() -> historyReads.get() >= 2 && statusLabel("Không thể tải tiền sử bệnh.") != null);
        assertEquals(0, historyRowCount());
        assertFalse(containsText(panel, "Previously displayed synthetic detail"));

        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(2, 2));
        await(() -> historyReads.get() >= 3 && statusLabel("Không thể tải tiền sử bệnh.") != null);
        assertEquals(0, historyRowCount());
        assertFalse(containsText(panel, "denied"));
        assertFalse(containsText(panel, "audit failed"));
    }

    @Test
    void olderPatientResponseCannotOverwriteNewerSelection() throws Exception {
        lifecycle = activeLifecycle();
        CountDownLatch oldRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseOldRequest = new CountDownLatch(1);
        panel = new DoctorStationPanel(lifecycle,
                query -> Result.success(List.of(queueItem(101, 501, "SYN-501"), queueItem(102, 502, "SYN-502"))),
                query -> {
                    if (query.patientId() == 501) {
                        oldRequestStarted.countDown();
                        awaitLatch(releaseOldRequest);
                        return Result.success(List.of(history(501, "Stale synthetic history")));
                    }
                    return Result.success(List.of(history(502, "Current synthetic history")));
                });
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 2);
        JTable queueTable = queueTable();

        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(0, 0));
        assertTrue(oldRequestStarted.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> queueTable.setRowSelectionInterval(1, 1));
        await(() -> historyRowCount() == 1
                && "Current synthetic history".equals(historyTable().getValueAt(0, 1)));

        releaseOldRequest.countDown();
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Current synthetic history", historyTable().getValueAt(0, 1));
        assertFalse(containsText(panel, "Stale synthetic history"));
    }

    @Test
    void sessionChangeClearsDataAndBlocksPendingHistoryResponse() throws Exception {
        lifecycle = activeLifecycle();
        CountDownLatch historyStarted = new CountDownLatch(1);
        CountDownLatch releaseHistory = new CountDownLatch(1);
        panel = new DoctorStationPanel(lifecycle,
                query -> Result.success(List.of(queueItem(101, 501, "SYN-501"))),
                query -> {
                    historyStarted.countDown();
                    awaitLatch(releaseHistory);
                    return Result.success(List.of(history(501, "Must not return after logout")));
                });
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 1);
        SwingUtilities.invokeAndWait(() -> queueTable().setRowSelectionInterval(0, 0));
        assertTrue(historyStarted.await(2, TimeUnit.SECONDS));

        lifecycle.clear();
        SwingUtilities.invokeAndWait(panel::monitorSession);
        assertEquals(0, queueTable().getRowCount());
        assertEquals(0, historyRowCount());
        releaseHistory.countDown();
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(0, historyRowCount());
        assertFalse(containsText(panel, "Must not return after logout"));
    }

    @Test
    void immediateSessionClearWithoutTimerTickCancelsWorkersAndClearsProtectedData() throws Exception {
        lifecycle = activeLifecycle();
        CountDownLatch historyStarted = new CountDownLatch(1);
        CountDownLatch releaseHistory = new CountDownLatch(1);
        panel = new DoctorStationPanel(lifecycle,
                query -> Result.success(List.of(queueItem(101, 501, "SYN-501"))),
                query -> {
                    historyStarted.countDown();
                    awaitLatch(releaseHistory);
                    return Result.success(List.of(history(501, "Protected history detail")));
                });
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 1);
        SwingUtilities.invokeAndWait(() -> queueTable().setRowSelectionInterval(0, 0));
        assertTrue(historyStarted.await(2, TimeUnit.SECONDS));

        SwingWorker<?, ?> activeWorker = panel.activeHistoryWorker();
        assertNotNull(activeWorker, "History worker must be active during background read");

        // Clear session: listener must immediately notify without waiting for monitorSession or 500ms timer
        lifecycle.clear();
        SwingUtilities.invokeAndWait(() -> {});

        assertTrue(activeWorker.isCancelled(), "Active history worker must be cancelled immediately");
        assertNull(panel.activeHistoryWorker());
        assertEquals(0, queueTable().getRowCount(), "Queue rows must be cleared immediately");
        assertEquals(0, historyRowCount(), "History rows must be cleared immediately");
        assertNotNull(statusLabel("Không thể tải hàng đợi."));
        assertNotNull(statusLabel("Không thể tải tiền sử bệnh."));

        // When the background read finishes, its done() must not repopulate UI
        releaseHistory.countDown();
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(0, historyRowCount());
        assertFalse(containsText(panel, "Protected history detail"));
    }

    @Test
    void replacementValidSessionSafelyReloadsThroughAuthorizedQueuePath() throws Exception {
        lifecycle = activeLifecycle();
        AtomicReference<GetDoctorQueueQuery> lastQueueQuery = new AtomicReference<>();
        panel = new DoctorStationPanel(lifecycle,
                query -> {
                    lastQueueQuery.set(query);
                    int doctorId = query.authorizationContext().employeeId();
                    return Result.success(List.of(queueItem(200 + doctorId, 600 + doctorId, "DOC-" + doctorId)));
                },
                query -> Result.success(List.of()));
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 1);
        assertEquals(701, lastQueueQuery.get().authorizationContext().employeeId());

        // Replace session with Doctor 702
        lifecycle.startSession(
                702, "synthetic-doctor-2", "Synthetic Doctor 2",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID().toString());

        // Must reload immediately through listener without waiting for monitorSession timer
        await(() -> lastQueueQuery.get() != null
                && lastQueueQuery.get().authorizationContext().employeeId() == 702
                && queueTable().getRowCount() == 1);

        JTable queueTable = queueTable();
        ReceptionQueueItem queued = (ReceptionQueueItem) queueTable.getModel().getValueAt(0, 0);
        assertEquals(200 + 702, queued.visitId());
        assertEquals(600 + 702, queued.patientId());
    }

    @Test
    void hidingPanelUnsubscribesAndCancelsPendingWorkers() throws Exception {
        lifecycle = activeLifecycle();
        CountDownLatch historyStarted = new CountDownLatch(1);
        CountDownLatch releaseHistory = new CountDownLatch(1);
        AtomicInteger queueLoads = new AtomicInteger();
        panel = new DoctorStationPanel(lifecycle,
                query -> {
                    queueLoads.incrementAndGet();
                    return Result.success(List.of(queueItem(101, 501, "SYN-501")));
                },
                query -> {
                    historyStarted.countDown();
                    awaitLatch(releaseHistory);
                    return Result.success(List.of(history(501, "Protected history")));
                });
        SwingUtilities.invokeAndWait(panel::onShow);
        await(() -> queueTable().getRowCount() == 1);
        SwingUtilities.invokeAndWait(() -> queueTable().setRowSelectionInterval(0, 0));
        assertTrue(historyStarted.await(2, TimeUnit.SECONDS));

        SwingWorker<?, ?> activeWorker = panel.activeHistoryWorker();
        assertNotNull(activeWorker);

        // Hide panel: cancels active workers and unsubscribes
        SwingUtilities.invokeAndWait(panel::onHide);
        assertTrue(activeWorker.isCancelled(), "Worker must be cancelled on hide");
        assertNull(panel.activeHistoryWorker());
        assertEquals(0, queueTable().getRowCount());
        assertEquals(0, historyRowCount());

        int loadsBeforeReplacement = queueLoads.get();
        // New session start while hidden must not trigger reload
        lifecycle.startSession(
                702, "synthetic-doctor-2", "Synthetic Doctor 2",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID().toString());
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(loadsBeforeReplacement, queueLoads.get(), "Hidden panel must not reload after unsubscribing");

        releaseHistory.countDown();
    }

    private JTable queueTable() {
        return findTable(panel, 3);
    }

    private JTable historyTable() {
        return findTable(panel, 2);
    }

    private int historyRowCount() {
        JTable table = historyTable();
        return table == null ? 0 : table.getRowCount();
    }

    private JLabel statusLabel(String text) {
        return findLabel(panel, text);
    }

    private static SessionLifecycleService activeLifecycle() {
        SessionLifecycleService sessionLifecycle = new SessionLifecycleService(
                UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC());
        sessionLifecycle.startSession(
                701, "synthetic-doctor", "Synthetic Doctor",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID().toString());
        return sessionLifecycle;
    }

    private static ReceptionQueueItem queueItem(int visitId, int patientId, String patientCode) {
        return new ReceptionQueueItem(visitId, patientId, patientCode, "Synthetic Patient " + patientId,
                31, "Synthetic Room", visitId, "Q-" + visitId, "CHO_KHAM", LocalDateTime.now());
    }

    private static TienSuBenh history(int patientId, String detail) {
        TienSuBenh history = new TienSuBenh();
        history.setMaBenhNhan(patientId);
        history.setLoaiTienSu(LoaiTienSu.DI_UNG);
        history.setMoTa(detail);
        history.setOwnershipScope(11, 21);
        return history;
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            AtomicBoolean satisfied = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) return;
            Thread.sleep(10);
        }
        fail("Timed out waiting for Swing worker result.");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test release.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test release.", exception);
        }
    }

    private static JTable findTable(Container container, int columns) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable table && table.getColumnCount() == columns) return table;
            if (component instanceof Container child) {
                JTable table = findTable(child, columns);
                if (table != null) return table;
            }
        }
        return null;
    }

    private static JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return label;
            if (component instanceof Container child) {
                JLabel label = findLabel(child, text);
                if (label != null) return label;
            }
        }
        return null;
    }

    private static boolean containsText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText() != null
                    && label.getText().contains(text)) return true;
            if (component instanceof Container child && containsText(child, text)) return true;
        }
        return false;
    }
}

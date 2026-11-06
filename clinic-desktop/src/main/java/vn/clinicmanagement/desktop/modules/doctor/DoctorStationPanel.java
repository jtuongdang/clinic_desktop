package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.patienthistory.GetPatientHistoryQuery;
import vn.clinicmanagement.application.reception.GetDoctorQueueQuery;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorActionBar;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorClinicalForm;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorHistoryPanel;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorQueueSidebar;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Doctor station read workflow for the scoped team queue and patient history. */
public class DoctorStationPanel extends BaseViewPanel implements SessionLifecycleService.SessionListener {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String UNAVAILABLE_MESSAGE = "Không thể tải dữ liệu bác sĩ trong phiên hiện tại.";

    private final SessionLifecycleService sessionLifecycle;
    private final Function<GetDoctorQueueQuery, Result<List<ReceptionQueueItem>>> queueReader;
    private final Function<GetPatientHistoryQuery, Result<List<TienSuBenh>>> historyReader;
    private final Timer sessionMonitor;
    private DoctorActionBar actionBar;
    private DoctorQueueSidebar queueSidebar;
    private DoctorClinicalForm clinicalForm;
    private DoctorHistoryPanel historyPanel;
    private AuthorizationContext observedSession;
    private volatile AuthorizationContext queueAuthorization;
    private ReceptionQueueItem selectedItem;
    private final AtomicLong requestGeneration = new AtomicLong();
    private SwingWorker<List<ReceptionQueueItem>, Void> activeQueueWorker;
    private SwingWorker<List<TienSuBenh>, Void> activeHistoryWorker;
    private boolean active;

    public DoctorStationPanel(SessionLifecycleService sessionLifecycle) {
        this(sessionLifecycle, DoctorCompositionRoot.create(sessionLifecycle));
    }

    private DoctorStationPanel(
            SessionLifecycleService sessionLifecycle,
            DoctorCompositionRoot.Services services) {
        this(sessionLifecycle, services.doctorQueue()::handle, services.patientHistory()::handle);
    }

    DoctorStationPanel(
            SessionLifecycleService sessionLifecycle,
            Function<GetDoctorQueueQuery, Result<List<ReceptionQueueItem>>> queueReader,
            Function<GetPatientHistoryQuery, Result<List<TienSuBenh>>> historyReader) {
        super("treatment", "Điều Trị", "");
        this.sessionLifecycle = Objects.requireNonNull(sessionLifecycle, "sessionLifecycle");
        this.queueReader = Objects.requireNonNull(queueReader, "queueReader");
        this.historyReader = Objects.requireNonNull(historyReader, "historyReader");
        this.sessionMonitor = new Timer(500, event -> monitorSession());
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        actionBar = new DoctorActionBar();
        actionBar.setOnRefresh(event -> refreshData());
        add(actionBar, BorderLayout.NORTH);

        queueSidebar = new DoctorQueueSidebar();
        queueSidebar.setOnPatientSelectedListener(this::onPatientChanged);

        clinicalForm = new DoctorClinicalForm();
        historyPanel = new DoctorHistoryPanel();
        JPanel patientTab = new JPanel(new BorderLayout(0, 4));
        patientTab.setBackground(Color.WHITE);
        patientTab.setBorder(new EmptyBorder(8, 12, 8, 12));
        patientTab.add(clinicalForm, BorderLayout.NORTH);
        patientTab.add(historyPanel, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AppFonts.BODY_BOLD);
        tabs.addTab("Bệnh nhân", patientTab);
        tabs.addTab("Công khám", createPlaceholderTab("Chưa có dữ liệu công khám được nối."));
        tabs.addTab("Thuốc", createPlaceholderTab("Chưa có dữ liệu đơn thuốc được nối."));
        tabs.addTab("Xử trí", createPlaceholderTab("Chưa có dữ liệu xử trí được nối."));
        tabs.addTab("Phản hồi", createPlaceholderTab("Chưa có dữ liệu cận lâm sàng được nối."));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, queueSidebar, tabs);
        split.setDividerLocation(280);
        split.setDividerSize(4);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onShow);
            return;
        }
        active = true;
        observedSession = currentAuthorizationContext();
        sessionLifecycle.addSessionListener(this);
        sessionMonitor.start();
        refreshData();
    }

    @Override
    public void onHide() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onHide);
            return;
        }
        active = false;
        sessionMonitor.stop();
        sessionLifecycle.removeSessionListener(this);
        observedSession = null;
        cancelActiveWorkers();
        clearProtectedData();
    }

    @Override
    public void onSessionChanged() {
        // Lifecycle events may arrive off the EDT; invalidate and interrupt before posting UI cleanup.
        cancelActiveWorkers();
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::handleSessionChange);
            return;
        }
        handleSessionChange();
    }

    @Override
    public void refreshData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshData);
            return;
        }

        cancelActiveWorkers();
        long generation = requestGeneration.incrementAndGet();
        selectedItem = null;
        actionBar.updatePatient(null);
        clinicalForm.updatePatient(null);
        historyPanel.clearHistory();
        queueSidebar.clearQueue();

        AuthorizationContext authorization = currentAuthorizationContext();
        observedSession = authorization;
        AuditActorContext auditActor = currentAuditActorContext();
        if (authorization == null || !authorization.isValid() || !matchesActor(authorization, auditActor)) {
            queueSidebar.showUnavailable();
            historyPanel.showUnavailable();
            return;
        }

        LocalDate businessDate = LocalDate.now(BUSINESS_ZONE);
        queueSidebar.showLoading(businessDate);
        GetDoctorQueueQuery query = new GetDoctorQueueQuery(businessDate, authorization, auditActor);
        SwingWorker<List<ReceptionQueueItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ReceptionQueueItem> doInBackground() {
                if (Thread.currentThread().isInterrupted()) {
                    return List.of();
                }
                Result<List<ReceptionQueueItem>> result = queueReader.apply(query);
                if (Thread.currentThread().isInterrupted()) {
                    return List.of();
                }
                if (result == null || result.isFailure()) {
                    throw new ProtectedReadFailure();
                }
                return List.copyOf(result.valueOrThrow());
            }

            @Override
            protected void done() {
                if (isCancelled() || generation != requestGeneration.get()) return;
                if (!isCurrentSession(authorization)) {
                    handleSessionChange();
                    return;
                }
                try {
                    queueSidebar.setQueue(get(), businessDate);
                    queueAuthorization = authorization;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    queueSidebar.showUnavailable();
                } catch (ExecutionException | CancellationException exception) {
                    queueSidebar.showUnavailable();
                } catch (RuntimeException exception) {
                    queueSidebar.showUnavailable();
                } finally {
                    synchronized (DoctorStationPanel.this) {
                        if (activeQueueWorker == this) {
                            activeQueueWorker = null;
                        }
                    }
                }
            }
        };
        synchronized (this) {
            if (generation != requestGeneration.get()) return;
            activeQueueWorker = worker;
        }
        worker.execute();
    }

    /** Clears protected queue and history data immediately, such as when the dashboard locks or logs out. */
    public void clearSensitiveData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clearSensitiveData);
            return;
        }
        cancelActiveWorkers();
        clearProtectedData();
        queueSidebar.showUnavailable();
        historyPanel.showUnavailable();
    }

    private void onPatientChanged(ReceptionQueueItem item) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onPatientChanged(item));
            return;
        }
        cancelActiveHistoryWorker();
        long generation = requestGeneration.incrementAndGet();
        AuthorizationContext authorization = currentAuthorizationContext();
        AuditActorContext auditActor = currentAuditActorContext();
        if (item != null && (authorization == null
                || !authorization.isValid()
                || !authorization.equals(queueAuthorization)
                || !matchesActor(authorization, auditActor))) {
            handleSessionChange();
            return;
        }

        selectedItem = item;
        historyPanel.clearHistory();
        actionBar.updatePatient(item);
        clinicalForm.updatePatient(item);
        if (item == null) return;

        historyPanel.showLoading();
        GetPatientHistoryQuery query = new GetPatientHistoryQuery(item.patientId(), authorization, auditActor);
        SwingWorker<List<TienSuBenh>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TienSuBenh> doInBackground() {
                if (Thread.currentThread().isInterrupted()) {
                    return List.of();
                }
                Result<List<TienSuBenh>> result = historyReader.apply(query);
                if (Thread.currentThread().isInterrupted()) {
                    return List.of();
                }
                if (result == null || result.isFailure()) {
                    throw new ProtectedReadFailure();
                }
                return List.copyOf(result.valueOrThrow());
            }

            @Override
            protected void done() {
                if (isCancelled()
                        || generation != requestGeneration.get()
                        || selectedItem == null
                        || selectedItem.patientId() != item.patientId()) {
                    return;
                }
                if (!isCurrentSession(authorization)) {
                    handleSessionChange();
                    return;
                }
                try {
                    List<TienSuBenh> histories = get();
                    if (!isValidHistory(histories, item.patientId(), authorization)) {
                        historyPanel.showUnavailable();
                        return;
                    }
                    historyPanel.showHistory(histories);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    historyPanel.showUnavailable();
                } catch (ExecutionException | CancellationException exception) {
                    historyPanel.showUnavailable();
                } catch (RuntimeException exception) {
                    historyPanel.showUnavailable();
                } finally {
                    synchronized (DoctorStationPanel.this) {
                        if (activeHistoryWorker == this) {
                            activeHistoryWorker = null;
                        }
                    }
                }
            }
        };
        synchronized (this) {
            if (generation != requestGeneration.get()) return;
            activeHistoryWorker = worker;
        }
        worker.execute();
    }

    void monitorSession() {
        AuthorizationContext current = currentAuthorizationContext();
        if (Objects.equals(observedSession, current)) return;
        handleSessionChange();
    }

    private void handleSessionChange() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::handleSessionChange);
            return;
        }
        cancelActiveWorkers();
        AuthorizationContext current = currentAuthorizationContext();
        observedSession = current;
        clearProtectedData();
        if (current != null && (active || isShowing())) {
            refreshData();
        } else {
            queueSidebar.showUnavailable();
            historyPanel.showUnavailable();
        }
    }

    private synchronized void cancelActiveWorkers() {
        queueAuthorization = null;
        requestGeneration.incrementAndGet();
        if (activeQueueWorker != null) {
            if (!activeQueueWorker.isDone()) {
                activeQueueWorker.cancel(true);
            }
            activeQueueWorker = null;
        }
        if (activeHistoryWorker != null) {
            if (!activeHistoryWorker.isDone()) {
                activeHistoryWorker.cancel(true);
            }
            activeHistoryWorker = null;
        }
    }

    private synchronized void cancelActiveHistoryWorker() {
        requestGeneration.incrementAndGet();
        if (activeHistoryWorker != null) {
            if (!activeHistoryWorker.isDone()) {
                activeHistoryWorker.cancel(true);
            }
            activeHistoryWorker = null;
        }
    }

    synchronized SwingWorker<List<ReceptionQueueItem>, Void> activeQueueWorker() {
        return activeQueueWorker;
    }

    synchronized SwingWorker<List<TienSuBenh>, Void> activeHistoryWorker() {
        return activeHistoryWorker;
    }

    private void clearProtectedData() {
        cancelActiveWorkers();
        selectedItem = null;
        if (actionBar != null) actionBar.updatePatient(null);
        if (clinicalForm != null) clinicalForm.updatePatient(null);
        if (historyPanel != null) historyPanel.clearHistory();
        if (queueSidebar != null) queueSidebar.clearQueue();
    }

    private AuthorizationContext currentAuthorizationContext() {
        Optional<AuthorizationContext> context = sessionLifecycle.currentAuthorizationContext();
        return context.orElse(null);
    }

    private AuditActorContext currentAuditActorContext() {
        return sessionLifecycle.currentAuditActorContext().orElse(null);
    }

    private boolean isCurrentSession(AuthorizationContext expected) {
        return expected.equals(currentAuthorizationContext());
    }

    private static boolean matchesActor(AuthorizationContext authorization, AuditActorContext actor) {
        return actor != null
                && authorization.employeeId() == actor.employeeId()
                && authorization.roleId() == actor.roleId()
                && Objects.equals(authorization.roleCode(), actor.roleCode())
                && authorization.facilityId() == actor.facilityId()
                && authorization.departmentId() == actor.departmentId();
    }

    private static boolean isValidHistory(
            List<TienSuBenh> histories, int patientId, AuthorizationContext authorization) {
        for (TienSuBenh history : histories) {
            LoaiTienSu category = history.getLoaiTienSu();
            if (history.getMaBenhNhan() != patientId
                    || !history.hasExactOwnershipScope(authorization.facilityId(), authorization.departmentId())
                    || history.isDeleted()
                    || (category != LoaiTienSu.BENH_NEN
                    && category != LoaiTienSu.DI_UNG
                    && category != LoaiTienSu.PHAU_THUAT)) {
                return false;
            }
        }
        return true;
    }

    private JPanel createPlaceholderTab(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private static final class ProtectedReadFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

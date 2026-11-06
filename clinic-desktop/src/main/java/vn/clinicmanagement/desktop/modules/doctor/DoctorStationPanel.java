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
import java.util.function.Function;

/** Doctor station read workflow for the scoped team queue and patient history. */
public class DoctorStationPanel extends BaseViewPanel {
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
    private ReceptionQueueItem selectedItem;
    private long requestGeneration;

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
        observedSession = currentAuthorizationContext();
        sessionMonitor.start();
        refreshData();
    }

    @Override
    public void onHide() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onHide);
            return;
        }
        sessionMonitor.stop();
        observedSession = null;
        clearProtectedData();
    }

    @Override
    public void refreshData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshData);
            return;
        }

        long generation = ++requestGeneration;
        selectedItem = null;
        actionBar.updatePatient(null);
        clinicalForm.updatePatient(null);
        historyPanel.clearHistory();
        queueSidebar.clearQueue();

        AuthorizationContext authorization = currentAuthorizationContext();
        observedSession = authorization;
        AuditActorContext auditActor = currentAuditActorContext();
        if (authorization == null || !matchesActor(authorization, auditActor)) {
            queueSidebar.showUnavailable();
            historyPanel.showUnavailable();
            return;
        }

        LocalDate businessDate = LocalDate.now(BUSINESS_ZONE);
        queueSidebar.showLoading(businessDate);
        GetDoctorQueueQuery query = new GetDoctorQueueQuery(businessDate, authorization, auditActor);
        new SwingWorker<List<ReceptionQueueItem>, Void>() {
            @Override
            protected List<ReceptionQueueItem> doInBackground() {
                Result<List<ReceptionQueueItem>> result = queueReader.apply(query);
                if (result == null || result.isFailure()) {
                    throw new ProtectedReadFailure();
                }
                return List.copyOf(result.valueOrThrow());
            }

            @Override
            protected void done() {
                if (generation != requestGeneration) return;
                if (!isCurrentSession(authorization)) {
                    handleSessionChange();
                    return;
                }
                try {
                    queueSidebar.setQueue(get(), businessDate);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    queueSidebar.showUnavailable();
                } catch (ExecutionException | CancellationException exception) {
                    queueSidebar.showUnavailable();
                } catch (RuntimeException exception) {
                    queueSidebar.showUnavailable();
                }
            }
        }.execute();
    }

    /** Clears protected queue and history data immediately, such as when the dashboard locks or logs out. */
    public void clearSensitiveData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clearSensitiveData);
            return;
        }
        clearProtectedData();
    }

    private void onPatientChanged(ReceptionQueueItem item) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onPatientChanged(item));
            return;
        }
        long generation = ++requestGeneration;
        selectedItem = item;
        historyPanel.clearHistory();
        actionBar.updatePatient(item);
        clinicalForm.updatePatient(item);
        if (item == null) return;

        AuthorizationContext authorization = currentAuthorizationContext();
        AuditActorContext auditActor = currentAuditActorContext();
        observedSession = authorization;
        if (authorization == null || !matchesActor(authorization, auditActor)) {
            handleSessionChange();
            return;
        }

        GetPatientHistoryQuery query = new GetPatientHistoryQuery(item.patientId(), authorization, auditActor);
        new SwingWorker<List<TienSuBenh>, Void>() {
            @Override
            protected List<TienSuBenh> doInBackground() {
                Result<List<TienSuBenh>> result = historyReader.apply(query);
                if (result == null || result.isFailure()) {
                    throw new ProtectedReadFailure();
                }
                return List.copyOf(result.valueOrThrow());
            }

            @Override
            protected void done() {
                if (generation != requestGeneration
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
                }
            }
        }.execute();
    }

    void monitorSession() {
        AuthorizationContext current = currentAuthorizationContext();
        if (Objects.equals(observedSession, current)) return;
        observedSession = current;
        clearProtectedData();
        if (current != null && isShowing()) refreshData();
        else if (current == null) {
            queueSidebar.showUnavailable();
            historyPanel.showUnavailable();
        }
    }

    private void handleSessionChange() {
        AuthorizationContext current = currentAuthorizationContext();
        observedSession = current;
        clearProtectedData();
        queueSidebar.showUnavailable();
        historyPanel.showUnavailable();
        if (current != null && isShowing()) refreshData();
    }

    private void clearProtectedData() {
        requestGeneration++;
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

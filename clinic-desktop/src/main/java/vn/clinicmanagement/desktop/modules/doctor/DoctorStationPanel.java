package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.GetDoctorQueueQuery;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorActionBar;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorClinicalForm;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorHistoryPanel;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorQueueSidebar;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Màn hình Bàn Khám Bác Sĩ & Quản Lý Điều Trị (Doctor Station)
 * Đóng gói độc lập trong package vn.clinicmanagement.desktop.modules.doctor
 * Tách biệt hoàn toàn với Admin và các phân hệ khác để dễ mở rộng & bảo trì.
 */
public class DoctorStationPanel extends BaseViewPanel {

    private final List<PatientExamDto> patientList = new ArrayList<>();
    private final DoctorCompositionRoot.Services services;
    private final SessionLifecycleService sessionLifecycleService;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong viewSessionEpoch = new AtomicLong();

    private DoctorActionBar actionBar;
    private DoctorQueueSidebar queueSidebar;
    private DoctorClinicalForm clinicalForm;
    private DoctorHistoryPanel historyPanel;

    public DoctorStationPanel() {
        this(resolveDefaultServices(), null);
    }

    public DoctorStationPanel(DoctorCompositionRoot.Services services) {
        this(services, null);
    }

    public DoctorStationPanel(DoctorCompositionRoot.Services services, SessionLifecycleService sessionLifecycleService) {
        super("treatment", "Điều Trị", "👨‍⚕️");
        this.services = services;
        this.sessionLifecycleService = sessionLifecycleService;
        buildUI();
    }

    private static DoctorCompositionRoot.Services resolveDefaultServices() {
        try {
            return DoctorCompositionRoot.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Top Action Bar + Banner tóm tắt bệnh nhân
        actionBar = new DoctorActionBar();
        add(actionBar, BorderLayout.NORTH);

        // 2. Main Split: Cột trái (Hàng đợi) + Cột phải (Hồ sơ khám)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);

        // Cột trái
        queueSidebar = new DoctorQueueSidebar(patientList);
        queueSidebar.setOnPatientSelectedListener(this::onPatientChanged);
        mainSplit.setLeftComponent(queueSidebar);

        // Cột phải
        JPanel rightWorkPanel = new JPanel(new BorderLayout());
        rightWorkPanel.setBackground(Color.WHITE);

        JTabbedPane clinicalTabs = new JTabbedPane();
        clinicalTabs.setFont(AppFonts.BODY_BOLD);

        // Tab "Bệnh Nhân"
        JPanel patientTab = new JPanel(new BorderLayout(0, 4));
        patientTab.setBackground(Color.WHITE);
        patientTab.setBorder(new EmptyBorder(8, 12, 8, 12));

        clinicalForm = new DoctorClinicalForm();
        patientTab.add(clinicalForm, BorderLayout.NORTH);

        historyPanel = new DoctorHistoryPanel();
        patientTab.add(historyPanel, BorderLayout.CENTER);

        clinicalTabs.addTab("Bệnh Nhân", patientTab);
        clinicalTabs.addTab("Công Khám(1)", createPlaceholderTab("Nội dung chỉ định dịch vụ công khám & thủ thuật"));
        clinicalTabs.addTab("Thuốc(2)", createPlaceholderTab("Danh sách thuốc & đơn thuốc ngoại trú / nội trú"));
        clinicalTabs.addTab("Xử Trí", createPlaceholderTab("Phương án điều trị, phẫu thuật, chuyển viện hoặc hẹn tái khám"));
        clinicalTabs.addTab("Phản Hồi", createPlaceholderTab("Phản hồi lâm sàng & kết quả chẩn đoán cận lâm sàng"));

        rightWorkPanel.add(clinicalTabs, BorderLayout.CENTER);
        mainSplit.setRightComponent(rightWorkPanel);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void onPatientChanged(PatientExamDto p) {
        actionBar.updatePatient(p);
        clinicalForm.updatePatient(p);
        historyPanel.updateHistory(p);
    }

    private JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("<html><center><h3>" + desc + "</h3><p style='color:gray'>Sẵn sàng kết nối dữ liệu khám bệnh EMR & chỉ định thuốc</p></center></html>", SwingConstants.CENTER);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (services == null || services.doctorQueue() == null) {
            queueSidebar.setPatients(List.of());
            onPatientChanged(null);
            return;
        }

        cancelInFlightWorkers();

        AuthorizationContext authzContext = currentAuthorizationContext();
        AuditActorContext auditContext = currentAuditActorContext();
        if (authzContext == null) {
            queueSidebar.setPatients(List.of());
            onPatientChanged(null);
            return;
        }

        long epoch = viewSessionEpoch.get();
        long sequence = refreshSequence.incrementAndGet();

        SwingWorker<Result<List<ReceptionQueueItem>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Result<List<ReceptionQueueItem>> doInBackground() {
                GetDoctorQueueQuery query = new GetDoctorQueueQuery(
                        LocalDate.now(), authzContext, auditContext);
                return services.doctorQueue().handle(query);
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (epoch != viewSessionEpoch.get() || sequence != refreshSequence.get() || isCancelled()) {
                    return;
                }
                try {
                    Result<List<ReceptionQueueItem>> result = get();
                    if (result != null && result.isSuccess()) {
                        List<ReceptionQueueItem> items = result.getValue().orElse(List.of());
                        List<PatientExamDto> mapped = new ArrayList<>();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                        for (ReceptionQueueItem item : items) {
                            if (item == null) continue;
                            String regTime = item.registeredAt() != null
                                    ? item.registeredAt().format(formatter)
                                    : "";
                            mapped.add(new PatientExamDto(
                                    item.patientName(),
                                    item.patientCode() != null ? item.patientCode() : "",
                                    "-",
                                    "-",
                                    "-",
                                    "",
                                    "-",
                                    "-",
                                    "",
                                    regTime,
                                    authzContext.roleCode(),
                                    item.roomName() != null ? item.roomName() : "",
                                    item.status() != null ? item.status() : "",
                                    ""
                            ));
                        }
                        queueSidebar.setPatients(mapped);
                    } else {
                        queueSidebar.setPatients(List.of());
                        onPatientChanged(null);
                    }
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal worker cancellation
                } catch (ExecutionException ex) {
                    queueSidebar.setPatients(List.of());
                    onPatientChanged(null);
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    public void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> worker : inFlightWorkers) {
            worker.cancel(true);
        }
        inFlightWorkers.clear();
    }

    public int getInFlightWorkersCount() {
        return inFlightWorkers.size();
    }

    public DoctorCompositionRoot.Services getServices() {
        return services;
    }

    public DoctorQueueSidebar getQueueSidebar() {
        return queueSidebar;
    }

    public DoctorActionBar getActionBar() {
        return actionBar;
    }

    public DoctorClinicalForm getClinicalForm() {
        return clinicalForm;
    }

    public DoctorHistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    private AuthorizationContext currentAuthorizationContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuthorizationContext().orElse(null);
        }
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "DOCTOR";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        UUID sessionId;
        try {
            sessionId = session.getSessionId() != null ? UUID.fromString(session.getSessionId()) : UUID.randomUUID();
        } catch (Exception ex) {
            sessionId = UUID.randomUUID();
        }
        return new AuthorizationContext(employeeId, roleId, roleCode, facilityId, departmentId, sessionId);
    }

    private AuditActorContext currentAuditActorContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuditActorContext().orElse(null);
        }
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "DOCTOR";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        String workstationId = session.getWorkstationId();
        if (workstationId == null || workstationId.isBlank()) {
            workstationId = "DOCTOR-01";
        }
        return new AuditActorContext(employeeId, roleId, roleCode, facilityId, departmentId, workstationId, UUID.randomUUID());
    }
}

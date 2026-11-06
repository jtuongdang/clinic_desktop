package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsHandler;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsQuery;
import vn.clinicmanagement.application.reception.GetReceptionQueueHandler;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.ReceptionQueueSnapshot;
import vn.clinicmanagement.application.reception.RegisterPatientHandler;
import vn.clinicmanagement.application.reception.SearchPatientsHandler;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.reception.components.ClinicRoomStatusPanel;
import vn.clinicmanagement.desktop.modules.reception.components.MedicalPopupChooser;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionFooterBar;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionTopToolbar;
import vn.clinicmanagement.domain.facility.PhongKham;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Phân hệ Đón Tiếp & Đăng Ký Khám Bệnh (EHC Reception Station).
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - Thanh công cụ đỉnh (Menu, In, Làm mới, Bệnh nhân mới, Cửa đón tiếp 2, Gọi số tiếp theo)
 * - Khung nhập liệu hành chính bệnh nhân & thẻ BHYT
 * - Dải 9 Tab lâm sàng và 3 cột bảng trạng thái phòng khám
 * - Thanh phím tắt chân trang: F1 (Mới), F2 (Lưu), F3 (In phiếu), F4 (Thẻ KCB)
 */
public class ReceptionPanel extends BaseViewPanel {

    private final ReceptionTopToolbar topToolbar;
    private final PatientRegistrationFormPanel formPanel;
    private final ClinicRoomStatusPanel statusPanel;
    private final ReceptionFooterBar footerBar;

    private final ReceptionCompositionRoot compositionRoot;
    private final GetReceptionQueueHandler queueHandler;
    private final GetActiveReceptionRoomsHandler roomsHandler;
    private final RegisterPatientHandler registerPatientHandler;
    private final SearchPatientsHandler searchPatientsHandler;
    private final SessionLifecycleService sessionLifecycleService;
    private final SessionLifecycleService.SessionListener sessionListener;

    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();

    public ReceptionPanel() {
        this(new ReceptionCompositionRoot(), null);
    }

    public ReceptionPanel(ReceptionCompositionRoot compositionRoot) {
        this(compositionRoot, null);
    }

    public ReceptionPanel(ReceptionCompositionRoot compositionRoot, SessionLifecycleService sessionLifecycleService) {
        this(
                compositionRoot != null ? compositionRoot.getGetReceptionQueueHandler() : null,
                compositionRoot != null ? compositionRoot.getGetActiveReceptionRoomsHandler() : null,
                compositionRoot != null ? compositionRoot.getRegisterPatientHandler() : null,
                compositionRoot != null ? compositionRoot.getSearchPatientsHandler() : null,
                compositionRoot,
                sessionLifecycleService
        );
    }

    public ReceptionPanel(
            GetReceptionQueueHandler queueHandler,
            GetActiveReceptionRoomsHandler roomsHandler,
            RegisterPatientHandler registerPatientHandler,
            SearchPatientsHandler searchPatientsHandler,
            SessionLifecycleService sessionLifecycleService) {
        this(queueHandler, roomsHandler, registerPatientHandler, searchPatientsHandler, null, sessionLifecycleService);
    }

    private ReceptionPanel(
            GetReceptionQueueHandler queueHandler,
            GetActiveReceptionRoomsHandler roomsHandler,
            RegisterPatientHandler registerPatientHandler,
            SearchPatientsHandler searchPatientsHandler,
            ReceptionCompositionRoot compositionRoot,
            SessionLifecycleService sessionLifecycleService) {
        super("reception", "Đăng Ký Khám Bệnh", "📋");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        this.compositionRoot = compositionRoot;
        this.queueHandler = queueHandler;
        this.roomsHandler = roomsHandler;
        this.registerPatientHandler = registerPatientHandler;
        this.searchPatientsHandler = searchPatientsHandler;
        this.sessionLifecycleService = sessionLifecycleService;

        // 1. Thanh công cụ đỉnh
        topToolbar = new ReceptionTopToolbar();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Vùng trung tâm: Chia dọc (Trên: Form đăng ký; Dưới: Trạng thái phòng khám)
        formPanel = new PatientRegistrationFormPanel();
        statusPanel = new ClinicRoomStatusPanel();

        // Đồng bộ khi chọn phòng khám trên form -> Đổi màu đỏ phòng trong bảng trạng thái
        formPanel.setOnClinicRoomChanged(room -> statusPanel.setSelectedClinicRoom(room));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, statusPanel);
        splitPane.setDividerLocation(218);
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);

        // 3. Thanh phím tắt chân trang
        footerBar = new ReceptionFooterBar();
        add(footerBar, BorderLayout.SOUTH);

        // Wire toolbar buttons
        findButtonByText(topToolbar, "Làm Mới").ifPresent(b -> b.addActionListener(e -> refreshData()));
        findButtonByText(topToolbar, "Bệnh Nhân Mới").ifPresent(b -> b.addActionListener(e -> clearForm()));

        // Wire session lifecycle listener
        if (this.sessionLifecycleService != null) {
            this.sessionListener = this::handleSessionChanged;
            this.sessionLifecycleService.addSessionListener(this.sessionListener);
            handleSessionChanged();
        } else {
            this.sessionListener = null;
        }
    }

    private void handleSessionChanged() {
        if (sessionLifecycleService == null) return;
        SessionLifecycleService.State state = sessionLifecycleService.evaluate();
        if (state != SessionLifecycleService.State.ACTIVE) {
            cancelInFlightWorkers();
            runOnEdt(() -> {
                setFormEditable(false);
                if (state == SessionLifecycleService.State.LOGGED_OUT) {
                    clearForm();
                }
            });
        } else {
            runOnEdt(() -> setFormEditable(true));
        }
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (sessionLifecycleService != null && sessionLifecycleService.evaluate() != SessionLifecycleService.State.ACTIVE) {
            cancelInFlightWorkers();
            return;
        }

        AuthorizationContext authzContext = currentAuthorizationContext();
        AuditActorContext auditContext = currentAuditActorContext();

        SwingWorker<RefreshDataSnapshot, Void> worker = new SwingWorker<>() {
            @Override
            protected RefreshDataSnapshot doInBackground() {
                Result<ReceptionQueueSnapshot> queueResult = null;
                if (queueHandler != null) {
                    queueResult = queueHandler.handle(
                            new GetReceptionQueueQuery(LocalDate.now(), authzContext, auditContext));
                }
                Result<List<PhongKham>> roomsResult = null;
                if (roomsHandler != null) {
                    roomsResult = roomsHandler.handle(
                            new GetActiveReceptionRoomsQuery(authzContext, auditContext));
                }
                return new RefreshDataSnapshot(queueResult, roomsResult);
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (isCancelled()) {
                    return;
                }
                try {
                    RefreshDataSnapshot snapshot = get();
                    if (snapshot != null) {
                        if (snapshot.queueResult != null && snapshot.queueResult.isSuccess()) {
                            snapshot.queueResult.getValue().ifPresent(ReceptionPanel.this::updateQueueAndRoomSummaries);
                        }
                        if (snapshot.roomsResult != null && snapshot.roomsResult.isSuccess()) {
                            snapshot.roomsResult.getValue().ifPresent(ReceptionPanel.this::updateActiveRooms);
                        }
                    }
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Safe cancellation on session lock or close
                } catch (ExecutionException ex) {
                    // Handled safely without leaking internal exception to dialog
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    public void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> worker : inFlightWorkers) {
            if (!worker.isDone()) {
                worker.cancel(true);
            }
        }
        inFlightWorkers.clear();
    }

    public int getInFlightWorkersCount() {
        return inFlightWorkers.size();
    }

    public List<SwingWorker<?, ?>> getInFlightWorkers() {
        return List.copyOf(inFlightWorkers);
    }

    public void setFormEditable(boolean editable) {
        setComponentsEditable(formPanel, editable);
    }

    public boolean isFormEditable() {
        List<JTextField> textFields = findComponents(formPanel, JTextField.class);
        for (JTextField tf : textFields) {
            if (!tf.isEditable()) {
                return false;
            }
        }
        return true;
    }

    public void clearForm() {
        clearFormComponents(formPanel);
    }

    private void clearFormComponents(Component comp) {
        if (comp instanceof JTextField tf) {
            tf.setText("");
        } else if (comp instanceof JCheckBox cb) {
            cb.setSelected(false);
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                clearFormComponents(child);
            }
        }
    }

    private void setComponentsEditable(Component comp, boolean editable) {
        if (comp instanceof JTextField tf) {
            tf.setEditable(editable);
        } else if (comp instanceof JComboBox<?> || comp instanceof JCheckBox || comp instanceof JButton) {
            comp.setEnabled(editable);
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setComponentsEditable(child, editable);
            }
        }
    }

    private void updateQueueAndRoomSummaries(ReceptionQueueSnapshot snapshot) {
        if (snapshot == null) return;
        List<JTable> tables = findComponents(statusPanel, JTable.class);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        for (JTable table : tables) {
            if (table.getColumnCount() == 7 && snapshot.queue() != null) {
                // Queue table (Card 0: "DS Đăng Ký Khám")
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                for (ReceptionQueueItem item : snapshot.queue()) {
                    String timeStr = item.registeredAt() != null ? item.registeredAt().format(formatter) : "";
                    String regCol = !timeStr.isEmpty() ? timeStr : (item.queueNumberDisplay() != null ? item.queueNumberDisplay() : "");
                    String stt = item.queueNumberDisplay() != null && !item.queueNumberDisplay().isBlank()
                            ? item.queueNumberDisplay() : String.valueOf(item.queueNumber());
                    String status = item.status() != null ? item.status() : "Chờ khám";
                    model.addRow(new Object[]{regCol, stt, status, "", "", "", ""});
                }
            } else if (table.getColumnCount() == 4 && snapshot.rooms() != null && !snapshot.rooms().isEmpty()) {
                // Room status tables (Card 2: "Trạng Thái Phòng Khám" columns)
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                Map<String, ClinicRoomQueueSummary> summaryMap = snapshot.rooms().stream()
                        .filter(r -> r.roomName() != null)
                        .collect(Collectors.toMap(
                                r -> r.roomName().trim().toLowerCase(Locale.ROOT),
                                r -> r,
                                (a, b) -> a));

                for (int r = 0; r < model.getRowCount(); r++) {
                    Object val = model.getValueAt(r, 0);
                    if (val != null) {
                        String roomName = val.toString().trim().toLowerCase(Locale.ROOT);
                        ClinicRoomQueueSummary summary = summaryMap.get(roomName);
                        if (summary != null) {
                            model.setValueAt(summary.registeredCount(), r, 1);
                            model.setValueAt(summary.waitingCount(), r, 2);
                            model.setValueAt(summary.examiningCount(), r, 3);
                        }
                    }
                }
            }
        }
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    private void updateActiveRooms(List<PhongKham> rooms) {
        if (rooms == null || rooms.isEmpty()) return;
        String[] roomNames = rooms.stream()
                .map(PhongKham::getTenPhong)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        if (roomNames.length == 0) return;

        List<MedicalPopupChooser> choosers = findComponents(formPanel, MedicalPopupChooser.class);
        for (MedicalPopupChooser chooser : choosers) {
            try {
                Field listField = MedicalPopupChooser.class.getDeclaredField("list");
                listField.setAccessible(true);
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>) listField.get(chooser);
                if (list != null) {
                    list.setListData(roomNames);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private AuthorizationContext currentAuthorizationContext() {
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "RECEPTIONIST";
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
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "RECEPTIONIST";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        String workstationId = session.getWorkstationId();
        if (workstationId == null || workstationId.isBlank()) {
            workstationId = "RECEPTION-01";
        }
        return new AuditActorContext(employeeId, roleId, roleCode, facilityId, departmentId, workstationId, UUID.randomUUID());
    }

    public void destroy() {
        cancelInFlightWorkers();
        if (sessionLifecycleService != null && sessionListener != null) {
            sessionLifecycleService.removeSessionListener(sessionListener);
        }
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> findComponents(Component root, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        findComponentsRecursive(root, clazz, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> void findComponentsRecursive(Component comp, Class<T> clazz, List<T> result) {
        if (clazz.isInstance(comp)) {
            result.add((T) comp);
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                findComponentsRecursive(child, clazz, result);
            }
        }
    }

    private static Optional<JButton> findButtonByText(Container container, String textSubstring) {
        List<JButton> buttons = findComponents(container, JButton.class);
        for (JButton b : buttons) {
            if (b.getText() != null && b.getText().contains(textSubstring)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public ReceptionCompositionRoot getCompositionRoot() {
        return compositionRoot;
    }

    public GetReceptionQueueHandler getQueueHandler() {
        return queueHandler;
    }

    public GetActiveReceptionRoomsHandler getRoomsHandler() {
        return roomsHandler;
    }

    public RegisterPatientHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    public SearchPatientsHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    public SessionLifecycleService getSessionLifecycleService() {
        return sessionLifecycleService;
    }

    public PatientRegistrationFormPanel getFormPanel() {
        return formPanel;
    }

    public ClinicRoomStatusPanel getStatusPanel() {
        return statusPanel;
    }

    public ReceptionTopToolbar getTopToolbar() {
        return topToolbar;
    }

    public ReceptionFooterBar getFooterBar() {
        return footerBar;
    }

    private record RefreshDataSnapshot(
            Result<ReceptionQueueSnapshot> queueResult,
            Result<List<PhongKham>> roomsResult
    ) {}
}

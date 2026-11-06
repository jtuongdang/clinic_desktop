package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.modules.reception.components.ClinicRoomStatusPanel;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionFooterBar;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionTopToolbar;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phân hệ Đón Tiếp & Đăng Ký Khám Bệnh (EHC Reception Station).
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - Thanh công cụ đỉnh (Menu, In, Làm mới, Bệnh nhân mới, Cửa đón tiếp 2, Gọi số tiếp theo)
 * - Khung nhập liệu hành chính bệnh nhân & thẻ BHYT
 * - Dải 9 Tab lâm sàng và 3 cột bảng trạng thái phòng khám
 * - Thanh phím tắt chân trang: F1 (Mới), F2 (Lưu), F3 (In phiếu), F4 (Thẻ KCB)
 */
public class ReceptionPanel extends BaseViewPanel implements SessionLifecycleService.SessionListener {

    private static final String PATIENT_VALIDATION_MESSAGE =
            "Thông tin người bệnh chưa hợp lệ. Vui lòng kiểm tra lại các trường bắt buộc.";
    private static final String REGISTRATION_ERROR_FALLBACK =
            "Đã xảy ra lỗi khi đăng ký người bệnh. Vui lòng thử lại sau.";

    private final ReceptionTopToolbar topToolbar;
    private final PatientRegistrationFormPanel formPanel;
    private final ClinicRoomStatusPanel statusPanel;
    private final ReceptionFooterBar footerBar;
    private final ReceptionCompositionRoot.Services services;
    private final SessionLifecycleService sessionLifecycle;
    private final AtomicLong sessionGeneration = new AtomicLong();
    private final AtomicLong loadGeneration = new AtomicLong();
    private final AtomicLong searchGeneration = new AtomicLong();
    private final AtomicBoolean pendingLogout = new AtomicBoolean();
    private final AtomicBoolean pendingLock = new AtomicBoolean();
    private SwingWorker<ReceptionLoad, Void> loadWorker;
    private SwingWorker<List<PatientSearchItem>, Void> searchWorker;
    // Registration is deliberately excluded from read cancellation.
    private SwingWorker<Result<RegistrationReceipt>, Void> registrationWorker;
    private AuthorizationContext observedSession;
    private boolean locked;
    private boolean active;
    private JDialog activeDialog;

    public ReceptionPanel(SessionLifecycleService sessionLifecycle) {
        this(sessionLifecycle, ReceptionCompositionRoot.create(sessionLifecycle));
    }

    ReceptionPanel(SessionLifecycleService sessionLifecycle, ReceptionCompositionRoot.Services services) {
        super("reception", "Đăng Ký Khám Bệnh", "📋");
        this.sessionLifecycle = Objects.requireNonNull(sessionLifecycle, "sessionLifecycle");
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

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
        footerBar.setOnNew(formPanel::clearForm);
        footerBar.setOnSave(this::saveRegistration);
        add(footerBar, BorderLayout.SOUTH);

        formPanel.setOnSearch(this::searchPatients);
    }

    @Override
    public void onShow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onShow);
            return;
        }
        if (active) return;
        active = true;
        sessionLifecycle.addSessionListener(this);
        onSessionChanged();
    }

    @Override
    public void onHide() {
        // Invalidate even if the caller is not on the EDT.
        invalidateSession();
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onHide);
            return;
        }
        active = false;
        sessionLifecycle.removeSessionListener(this);
        clearProtectedReads();
    }

    @Override
    public void onSessionChanged() {
        // This happens in the callback, before any queued EDT cleanup or monitor tick.
        long generation = invalidateSession();
        var state = sessionLifecycle.state();
        var context = sessionLifecycle.currentAuthorizationContext().orElse(null);
        if (state == SessionLifecycleService.State.LOGGED_OUT) pendingLogout.set(true);
        if (state == SessionLifecycleService.State.LOCKED_IDLE
                || state == SessionLifecycleService.State.LOCKED_ABSOLUTE) pendingLock.set(true);
        Runnable cleanup = () -> handleSessionChange(state, context, generation);
        if (SwingUtilities.isEventDispatchThread()) cleanup.run();
        else SwingUtilities.invokeLater(cleanup);
    }

    private void handleSessionChange(SessionLifecycleService.State state, AuthorizationContext context,
                                     long generation) {
        // A later EDT notification can overtake a queued background notification. Retain the
        // logout/lock policy, but never let that older cleanup erase the later session's draft.
        if (generation != sessionGeneration.get()) return;
        boolean wasLocked = pendingLock.getAndSet(false) || locked;
        boolean loggedOut = pendingLogout.getAndSet(false) || state == SessionLifecycleService.State.LOGGED_OUT;
        locked = state == SessionLifecycleService.State.LOCKED_IDLE
                || state == SessionLifecycleService.State.LOCKED_ABSOLUTE;
        // Absolute reauthentication rotates the UUID while retaining the same draft owner.
        boolean resumedDraft = wasLocked && sameIdentityAndScope(observedSession, context);
        if (loggedOut
                || (context != null && observedSession != null
                && !context.equals(observedSession) && !resumedDraft)) {
            formPanel.clearForm();
            clearSearchInput(formPanel);
        }
        if (context != null || state == SessionLifecycleService.State.LOGGED_OUT) observedSession = context;
        clearProtectedReads();
        if (active && context != null && generation == sessionGeneration.get()) refreshData();
    }

    private static boolean sameIdentityAndScope(AuthorizationContext first, AuthorizationContext second) {
        return first != null && second != null && first.employeeId() == second.employeeId()
                && first.roleId() == second.roleId() && Objects.equals(first.roleCode(), second.roleCode())
                && first.facilityId() == second.facilityId() && first.departmentId() == second.departmentId();
    }

    private synchronized long invalidateSession() {
        long generation = sessionGeneration.incrementAndGet();
        cancelLoad();
        cancelSearch();
        return generation;
    }

    private synchronized void cancelLoad() {
        loadGeneration.incrementAndGet();
        var previous = loadWorker;
        loadWorker = null;
        if (previous != null) previous.cancel(true);
    }

    private synchronized void cancelSearch() {
        searchGeneration.incrementAndGet();
        var previous = searchWorker;
        searchWorker = null;
        if (previous != null) previous.cancel(true);
    }

    private void clearProtectedReads() {
        if (activeDialog != null) activeDialog.dispose();
        formPanel.setClinicRooms(List.of());
        statusPanel.setQueue(List.of());
        statusPanel.setRoomSummaries(List.of());
        statusPanel.setSelectedClinicRoom(null);
        topToolbar.setNextQueueNumber(null);
    }

    private void clearSearchInput(Container container) {
        // clearForm intentionally preserves the separate search bar; session replacement must clear it too.
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField field
                    && ("searchPhone".equals(field.getName()) || "searchCccd".equals(field.getName()))) {
                field.setText("");
            } else if (component instanceof Container child) clearSearchInput(child);
        }
    }

    private boolean isCurrentSession(long generation, AuthorizationContext context) {
        if (!active || generation != sessionGeneration.get()) return false;
        boolean matches = context.equals(sessionLifecycle.currentAuthorizationContext().orElse(null));
        return matches && generation == sessionGeneration.get();
    }

    private static boolean matchesActor(AuthorizationContext context, AuditActorContext actor) {
        return context != null && actor != null && context.employeeId() == actor.employeeId()
                && context.roleId() == actor.roleId() && context.roleCode().equalsIgnoreCase(actor.roleCode())
                && context.facilityId() == actor.facilityId() && context.departmentId() == actor.departmentId();
    }

    @Override
    public void refreshData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshData);
            return;
        }
        if (!active) return;
        cancelLoad();
        long generation = sessionGeneration.get();
        long request = loadGeneration.get();
        var authorizationContext = sessionLifecycle.currentAuthorizationContext().orElse(null);
        var queueAuditActor = sessionLifecycle.currentAuditActorContext().orElse(null);
        var catalogAuditActor = sessionLifecycle.currentAuditActorContext().orElse(null);
        if (!matchesActor(authorizationContext, queueAuditActor)
                || !matchesActor(authorizationContext, catalogAuditActor)
                || !isCurrentSession(generation, authorizationContext)) {
            clearProtectedReads();
            return;
        }
        formPanel.setClinicRooms(List.of());
        statusPanel.setQueue(List.of());
        statusPanel.setRoomSummaries(List.of());
        topToolbar.setNextQueueNumber(null);
        var date = java.time.LocalDate.now();
        SwingWorker<ReceptionLoad, Void> worker = new SwingWorker<>() {
            @Override protected ReceptionLoad doInBackground() {
                if (isCancelled()) return null;
                var roomsResult = services.getActiveRooms().handle(new GetActiveReceptionRoomsQuery(
                        authorizationContext, catalogAuditActor));
                if (isCancelled()) return null;
                var queueResult = services.getQueue().handle(new GetReceptionQueueQuery(date,
                        authorizationContext, queueAuditActor));
                if (roomsResult.isFailure() || queueResult.isFailure()) throw new IllegalStateException("Protected read failed.");
                return new ReceptionLoad(roomsResult.valueOrThrow(), queueResult.valueOrThrow());
            }
            @Override protected void done() {
                if (isCancelled() || request != loadGeneration.get()
                        || !isCurrentSession(generation, authorizationContext)) return;
                try {
                    ReceptionLoad load = get();
                    formPanel.setClinicRooms(load.rooms());
                    ReceptionQueueSnapshot snapshot = load.snapshot();
                    statusPanel.setQueue(snapshot.queue());
                    statusPanel.setRoomSummaries(snapshot.rooms());
                    topToolbar.setNextQueueNumber(snapshot.queue().isEmpty() ? null : snapshot.queue().get(0).queueNumberDisplay());
                } catch (Exception ex) {
                    if (isCurrentSession(generation, authorizationContext))
                        showError("Không thể tải phòng khám và hàng đợi. Kiểm tra kết nối cơ sở dữ liệu.");
                } finally {
                    synchronized (ReceptionPanel.this) {
                        if (loadWorker == this) loadWorker = null;
                    }
                }
            }
        };
        synchronized (this) {
            if (generation != sessionGeneration.get() || request != loadGeneration.get()) return;
            loadWorker = worker;
        }
        worker.execute();
    }

    private void searchPatients(String phone, String cccd) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> searchPatients(phone, cccd));
            return;
        }
        if (!active) return;
        cancelSearch();
        long generation = sessionGeneration.get();
        long request = searchGeneration.get();
        String first = phone == null ? "" : phone.trim();
        String second = cccd == null ? "" : cccd.trim();
        var context = sessionLifecycle.currentAuthorizationContext().orElse(null);
        var firstActor = sessionLifecycle.currentAuditActorContext().orElse(null);
        var secondActor = sessionLifecycle.currentAuditActorContext().orElse(null);
        if (!matchesActor(context, firstActor) || !matchesActor(context, secondActor)
                || !isCurrentSession(generation, context)) return;
        if (first.isBlank() && second.isBlank()) { showError("Nhập số điện thoại hoặc CCCD để tìm hồ sơ."); return; }
        SwingWorker<List<PatientSearchItem>, Void> worker = new SwingWorker<>() {
            @Override protected List<PatientSearchItem> doInBackground() {
                Map<Integer, PatientSearchItem> results = new LinkedHashMap<>();
                if (!first.isBlank() && !isCancelled()) addMatches(results, first, firstActor);
                if (!second.isBlank() && !second.equals(first) && !isCancelled()) addMatches(results, second, secondActor);
                return new ArrayList<>(results.values());
            }
            private void addMatches(Map<Integer, PatientSearchItem> results, String keyword, AuditActorContext actor) {
                Result<List<PatientSearchItem>> result = services.searchPatients().handle(
                        new SearchPatientsQuery(keyword, context, actor));
                if (result.isSuccess()) result.getValue().orElseThrow().forEach(item -> results.put(item.patient().getId(), item));
                else throw new IllegalStateException("Protected read failed.");
            }
            private boolean current() {
                return !isCancelled() && request == searchGeneration.get() && isCurrentSession(generation, context);
            }
            @Override protected void done() {
                if (!current()) return;
                try {
                    var matches = get();
                    if (matches.isEmpty()) {
                        showMessage("Không tìm thấy hồ sơ phù hợp. Dữ liệu đang nhập vẫn được giữ nguyên.",
                                "Tìm người bệnh", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    PatientSearchItem selected;
                    if (matches.size() == 1 && !formPanel.hasUnsavedInput()) selected = matches.get(0);
                    else selected = choosePatient(matches);
                    // A modal dialog runs a nested event loop: the session may change while it is open.
                    if (selected != null && current()) formPanel.selectPatient(selected.patient(), selected.latestInsurance());
                } catch (Exception ex) {
                    if (current()) showError("Không thể tìm hồ sơ người bệnh. Kiểm tra kết nối cơ sở dữ liệu.");
                } finally {
                    synchronized (ReceptionPanel.this) {
                        if (searchWorker == this) searchWorker = null;
                    }
                }
            }
        };
        synchronized (this) {
            if (generation != sessionGeneration.get() || request != searchGeneration.get()) return;
            searchWorker = worker;
        }
        worker.execute();
    }

    private void saveRegistration() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::saveRegistration);
            return;
        }
        if (!active || registrationWorker != null) return;
        long generation = sessionGeneration.get();
        var context = sessionLifecycle.currentAuthorizationContext().orElse(null);
        var auditActor = sessionLifecycle.currentAuditActorContext().orElse(null);
        if (!matchesActor(context, auditActor) || !isCurrentSession(generation, context)) return;
        final RegisterPatientCommand command;
        try { command = formPanel.toRegisterCommand(context, auditActor); }
        catch (IllegalArgumentException ex) { showError(registrationFailureMessage("PATIENT_VALIDATION")); return; }
        footerBar.setSaving(true);
        registrationWorker = new SwingWorker<>() {
            @Override protected Result<RegistrationReceipt> doInBackground() { return services.registerPatient().handle(command); }
            @Override protected void done() {
                registrationWorker = null;
                // Release the busy controls, including when the transaction finished under a later session.
                footerBar.setSaving(false);
                if (!isCurrentSession(generation, context)) return;
                try {
                    Result<RegistrationReceipt> result = get();
                    if (result.isFailure()) {
                        handleRegistrationFailure(result.getError().orElseThrow());
                        return;
                    }
                    RegistrationReceipt receipt = result.getValue().orElseThrow();
                    showMessage("Đã đăng ký người bệnh " + receipt.patientCode() + " · Số tiếp đón " + receipt.queueNumberDisplay(),
                            "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                    if (!isCurrentSession(generation, context)) return;
                    formPanel.clearForm();
                    refreshData();
                } catch (Exception ex) {
                    if (isCurrentSession(generation, context)) showError("Không thể lưu đăng ký. Vui lòng thử lại.");
                }
            }
        };
        registrationWorker.execute();
    }

    PatientSearchItem choosePatient(List<PatientSearchItem> matches) {
        JOptionPane pane = new JOptionPane("Chọn hồ sơ người bệnh hiện có (Hủy để giữ thông tin đang nhập):",
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        pane.setWantsInput(true);
        pane.setSelectionValues(matches.toArray());
        pane.setInitialSelectionValue(matches.get(0));
        displayDialog(pane, "Kết quả tìm kiếm");
        return pane.getInputValue() instanceof PatientSearchItem item ? item : null;
    }

    void showMessage(String message, String title, int type) {
        displayDialog(new JOptionPane(message, type), title);
    }

    private void displayDialog(JOptionPane pane, String title) {
        JDialog dialog = pane.createDialog(this, title);
        activeDialog = dialog;
        try { dialog.setVisible(true); }
        finally {
            dialog.dispose();
            if (activeDialog == dialog) activeDialog = null;
        }
    }

    private void handleRegistrationFailure(Result.ApplicationError error) {
        showError(registrationFailureMessage(error.code()));
    }

    static String registrationFailureMessage(String code) {
        if (code == null) return REGISTRATION_ERROR_FALLBACK;
        return switch (code) {
            case "PATIENT_VALIDATION" -> PATIENT_VALIDATION_MESSAGE;
            case "ROOM_UNAVAILABLE" -> "Phòng khám đã chọn không còn khả dụng. Vui lòng chọn phòng khám khác.";
            case "PATIENT_REGISTRATION_FAILED" -> "Không thể hoàn tất đăng ký người bệnh. Vui lòng thử lại.";
            default -> REGISTRATION_ERROR_FALLBACK;
        };
    }

    private static String sanitizeUserMessage(String message, String fallback) {
        if (message == null || message.isBlank()) return fallback;
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("exception")
                || lower.contains("unique") || lower.contains("constraint") || lower.contains("duplicate key")) {
            return fallback;
        }
        return message.replaceAll("\\b\\d{9,12}\\b", "***");
    }

    private void showError(String message) {
        showMessage(sanitizeUserMessage(message, "Đã xảy ra lỗi khi xử lý yêu cầu."), "Tiếp đón người bệnh", JOptionPane.ERROR_MESSAGE);
    }

    private record ReceptionLoad(java.util.List<PhongKham> rooms, ReceptionQueueSnapshot snapshot) {}
}

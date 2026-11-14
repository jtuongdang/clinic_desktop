package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongQuery;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiQuery;
import vn.clinicmanagement.application.reception.BenhNhanSearchMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiAnhChup;
import vn.clinicmanagement.application.reception.RegisterBenhNhanCommand;
import vn.clinicmanagement.application.reception.RegisterBenhNhanHandler;
import vn.clinicmanagement.application.reception.DangKyBienNhan;
import vn.clinicmanagement.application.reception.SearchBenhNhanHandler;
import vn.clinicmanagement.application.reception.SearchBenhNhanQuery;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.reception.components.PhongKhamPhongTrangThaiManHinh;
import vn.clinicmanagement.desktop.modules.reception.components.YTeBatLenBoChon;
import vn.clinicmanagement.desktop.modules.reception.components.BenhNhanDangKyBieuMauManHinh;
import vn.clinicmanagement.desktop.modules.reception.components.TiepDonChanTrangThanh;
import vn.clinicmanagement.desktop.modules.reception.components.TiepDonTrenThanhCongCu;
import vn.clinicmanagement.domain.facility.PhongKham;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phân hệ Đón Tiếp & Đăng Ký Khám Bệnh (EHC Reception Station).
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - Thanh công cụ đỉnh (Menu, In, Làm mới, Bệnh nhân mới, Cửa đón tiếp 2, Gọi số tiếp theo)
 * - Khung nhập liệu hành chính bệnh nhân & thẻ BHYT
 * - Dải 9 Tab lâm sàng và 3 cột bảng trạng thái phòng khám
 * - Thanh phím tắt chân trang: F1 (Mới), F2 (Lưu), F3 (In phiếu), F4 (Thẻ KCB)
 */
public class TiepDonManHinh extends CoSoManHinhManHinh {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Trường `topToolbar`: tren thanhcongcu. */
    private final TiepDonTrenThanhCongCu topToolbar;
    /** Trường `formPanel`: bieumau màn hình. */
    private final BenhNhanDangKyBieuMauManHinh formPanel;
    /** Trường `statusPanel`: trạng thái màn hình. */
    private final PhongKhamPhongTrangThaiManHinh statusPanel;
    /** Trường `footerBar`: chantrang thanh. */
    private final TiepDonChanTrangThanh footerBar;

    /** Trường `compositionRoot`: ghepnoi goc. */
    private final TiepDonGhepNoiGoc compositionRoot;
    /** Trường `queueHandler`: hangdoi xử lý. */
    private final GetTiepDonHangDoiHandler queueHandler;
    /** Trường `roomsHandler`: phòng xử lý. */
    private final GetHoatDongTiepDonPhongHandler roomsHandler;
    /** Trường `registerPatientHandler`: đăng ký bệnh nhân xử lý. */
    private final RegisterBenhNhanHandler registerPatientHandler;
    /** Trường `searchPatientsHandler`: tìm kiếm bệnh nhân xử lý. */
    private final SearchBenhNhanHandler searchPatientsHandler;
    /** Trường `sessionLifecycleService`: phien vongdoi dịch vụ. */
    private final PhienVongDoiDichVu sessionLifecycleService;
    /** Trường `sessionListener`: phien bolangnghe. */
    private final PhienVongDoiDichVu.SessionListener sessionListener;

    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    /** Trường `viewSessionEpoch`: màn hình phien `Epoch`. */
    private final AtomicLong viewSessionEpoch = new AtomicLong();
    /** Trường `refreshSequence`: làm mới `Sequence`. */
    private final AtomicLong refreshSequence = new AtomicLong();
    /** Trường `searchSequence`: tìm kiếm `Sequence`. */
    private final AtomicLong searchSequence = new AtomicLong();
    private volatile boolean disposed;
    /** Trường `queueClock`: hangdoi `Clock`. */
    private Clock queueClock = Clock.system(CLINIC_ZONE);

    // Headless test seams
    private volatile String lastErrorMessage;
    /** Trường `lastInfoMessage`: `last` `Info` thông báo. */
    private volatile String lastInfoMessage;
    /** Trường `errorListener`: lỗi bolangnghe. */
    private java.util.function.Consumer<String> errorListener;
    /** Trường `commandConsumerForTest`: lenh `Consumer` cho test. */
    private java.util.function.Consumer<RegisterBenhNhanCommand> commandConsumerForTest;

    public TiepDonManHinh() {
        this(new TiepDonGhepNoiGoc(), null);
    }

    public TiepDonManHinh(TiepDonGhepNoiGoc compositionRoot) {
        this(compositionRoot, null);
    }

    public TiepDonManHinh(TiepDonGhepNoiGoc compositionRoot, PhienVongDoiDichVu sessionLifecycleService) {
        this(
                compositionRoot != null ? compositionRoot.getGetReceptionQueueHandler() : null,
                compositionRoot != null ? compositionRoot.getGetActiveReceptionRoomsHandler() : null,
                compositionRoot != null ? compositionRoot.getRegisterPatientHandler() : null,
                compositionRoot != null ? compositionRoot.getSearchPatientsHandler() : null,
                compositionRoot,
                sessionLifecycleService
        );
    }

    public TiepDonManHinh(
            GetTiepDonHangDoiHandler queueHandler,
            GetHoatDongTiepDonPhongHandler roomsHandler,
            RegisterBenhNhanHandler registerPatientHandler,
            SearchBenhNhanHandler searchPatientsHandler,
            PhienVongDoiDichVu sessionLifecycleService) {
        this(queueHandler, roomsHandler, registerPatientHandler, searchPatientsHandler, null, sessionLifecycleService);
    }

    private TiepDonManHinh(
            GetTiepDonHangDoiHandler queueHandler,
            GetHoatDongTiepDonPhongHandler roomsHandler,
            RegisterBenhNhanHandler registerPatientHandler,
            SearchBenhNhanHandler searchPatientsHandler,
            TiepDonGhepNoiGoc compositionRoot,
            PhienVongDoiDichVu sessionLifecycleService) {
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
        topToolbar = new TiepDonTrenThanhCongCu();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Vùng trung tâm: Chia dọc (Trên: Form đăng ký; Dưới: Trạng thái phòng khám)
        formPanel = new BenhNhanDangKyBieuMauManHinh();
        statusPanel = new PhongKhamPhongTrangThaiManHinh();

        // Đồng bộ khi chọn phòng khám trên form -> Đổi màu đỏ phòng trong bảng trạng thái
        formPanel.setOnClinicRoomChanged(room -> statusPanel.setSelectedClinicRoom(room));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, statusPanel);
        splitPane.setDividerLocation(218);
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);

        // 3. Thanh phím tắt chân trang
        footerBar = new TiepDonChanTrangThanh();
        add(footerBar, BorderLayout.SOUTH);

        // Wire toolbar buttons and action listeners
        topToolbar.setOnRefresh(this::refreshData);
        topToolbar.setOnNew(this::clearForm);
        footerBar.setOnSave(this::saveRegistration);
        footerBar.setOnNew(this::clearForm);
        formPanel.setOnSearch(this::searchPatients);

        // Wire session lifecycle listener
        if (this.sessionLifecycleService != null) {
            this.sessionListener = this::handleSessionChanged;
            this.sessionLifecycleService.addSessionListener(this.sessionListener);
            handleSessionChanged();
        } else {
            this.sessionListener = null;
        }
    }

    /** Hàm `handleSessionChanged`: `handle` phien thaydoi. */
    private void handleSessionChanged() {
        if (sessionLifecycleService == null) return;
        viewSessionEpoch.incrementAndGet();
        PhienVongDoiDichVu.State state = sessionLifecycleService.evaluate();
        if (state != PhienVongDoiDichVu.State.ACTIVE) {
            cancelInFlightWorkers();
            runOnEdt(() -> {
                setFormEditable(false);
                if (state == PhienVongDoiDichVu.State.LOGGED_OUT) {
                    clearForm();
                }
            });
        } else {
            runOnEdt(() -> setFormEditable(true));
        }
    }

    /** Hàm `onShow`: tải `Show`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onShow`. */
    @Override
    public void onShow() {
        refreshData();
    }

    /** Hàm `onHide`: tải `Hide`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onHide`. */
    @Override
    public void onHide() {
        viewSessionEpoch.incrementAndGet();
        cancelInFlightWorkers();
    }

    /** Hàm `setQueueClock`: gán hangdoi `Clock`. */
    void setQueueClock(Clock clock) {
        queueClock = clock.withZone(CLINIC_ZONE);
    }

    /** Hàm `refreshData`: làm mới dữ liệu. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#refreshData`. */
    @Override
    public void refreshData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshData);
            return;
        }
        if (disposed) return;
        if (sessionLifecycleService != null && sessionLifecycleService.evaluate() != PhienVongDoiDichVu.State.ACTIVE) {
            cancelInFlightWorkers();
            return;
        }

        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null || auditContext == null) return;
        long epoch = viewSessionEpoch.get();
        long sequence = refreshSequence.incrementAndGet();
        Clock clock = queueClock;

        SwingWorker<RefreshDataSnapshot, Void> worker = new SwingWorker<>() {
            @Override
            protected RefreshDataSnapshot doInBackground() {
                KetQua<TiepDonHangDoiAnhChup> queueResult = null;
                if (queueHandler != null) {
                    queueResult = queueHandler.handle(
                            new GetTiepDonHangDoiQuery(LocalDate.now(clock), authzContext, freshAuditContext(auditContext)));
                }
                KetQua<List<PhongKham>> roomsResult = null;
                if (roomsHandler != null) {
                    roomsResult = roomsHandler.handle(
                            new GetHoatDongTiepDonPhongQuery(authzContext, freshAuditContext(auditContext)));
                }
                return new RefreshDataSnapshot(queueResult, roomsResult);
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (!isCurrentWorker(epoch) || sequence != refreshSequence.get() || isCancelled()) return;
                try {
                    RefreshDataSnapshot snapshot = get();
                    if (snapshot != null) {
                        if (snapshot.queueResult != null && snapshot.queueResult.isSuccess()) {
                            snapshot.queueResult.getValue().ifPresent(TiepDonManHinh.this::updateQueueAndRoomSummaries);
                        } else if (snapshot.queueResult != null && snapshot.queueResult.isFailure()) {
                            TiepDonManHinh.this.clearQueueAndRoomSummaries();
                            showError(TiepDonManHinhThongBaoChinhSach.toUserMessage(snapshot.queueResult));
                        }
                        if (snapshot.roomsResult != null && snapshot.roomsResult.isSuccess()) {
                            TiepDonManHinh.this.updateActiveRooms(snapshot.roomsResult.getValue().orElse(List.of()));
                        } else if (snapshot.roomsResult != null && snapshot.roomsResult.isFailure()) {
                            TiepDonManHinh.this.updateActiveRooms(List.of());
                            showError(TiepDonManHinhThongBaoChinhSach.toUserMessage(snapshot.roomsResult));
                        }
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Safe cancellation on session lock or close
                } catch (ExecutionException ex) {
                    TiepDonManHinh.this.clearQueueAndRoomSummaries();
                    TiepDonManHinh.this.updateActiveRooms(List.of());
                    showError(TiepDonManHinhThongBaoChinhSach.MSG_RECEPTION_QUEUE_FAILED);
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    /** Hàm `cancelInFlightWorkers`: hủy trong `Flight` `Workers`. */
    public void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> worker : inFlightWorkers) {
            if (!worker.isDone()) {
                worker.cancel(true);
            }
        }
        inFlightWorkers.clear();
    }

    /** Hàm `getInFlightWorkersCount`: lấy trong `Flight` `Workers` dem. */
    public int getInFlightWorkersCount() {
        return inFlightWorkers.size();
    }

    /** Hàm `getInFlightWorkers`: lấy trong `Flight` `Workers`. */
    public List<SwingWorker<?, ?>> getInFlightWorkers() {
        return List.copyOf(inFlightWorkers);
    }

    /** Hàm `setFormEditable`: gán bieumau `Editable`. */
    public void setFormEditable(boolean editable) {
        setComponentsEditable(formPanel, editable);
    }

    /** Hàm `isFormEditable`: là bieumau `Editable`. */
    public boolean isFormEditable() {
        List<JTextField> textFields = findComponents(formPanel, JTextField.class);
        for (JTextField tf : textFields) {
            if (!tf.isEditable()) {
                return false;
            }
        }
        return true;
    }

    /** Hàm `clearForm`: xóa bieumau. */
    public void clearForm() {
        viewSessionEpoch.incrementAndGet();
        formPanel.clearForm();
        footerBar.setSaving(false);
    }

    /** Hàm `setComponentsEditable`: gán `Components` `Editable`. */
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

    /** Hàm `updateQueueAndRoomSummaries`: cập nhật hangdoi va phòng `Summaries`. */
    private void updateQueueAndRoomSummaries(TiepDonHangDoiAnhChup snapshot) {
        if (snapshot == null) return;
        List<JTable> tables = findComponents(statusPanel, JTable.class);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        for (JTable table : tables) {
            if (table.getColumnCount() == 7 && snapshot.queue() != null) {
                // Queue table (Card 0: "DS Đăng Ký Khám")
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                for (TiepDonHangDoiMatHang item : snapshot.queue()) {
                    String timeStr = item.registeredAt() != null ? item.registeredAt().format(formatter) : "";
                    String regCol = !timeStr.isEmpty() ? timeStr : (item.queueNumberDisplay() != null ? item.queueNumberDisplay() : "");
                    String stt = item.queueNumberDisplay() != null && !item.queueNumberDisplay().isBlank()
                            ? item.queueNumberDisplay() : String.valueOf(item.queueNumber());
                    String status = item.status() != null ? item.status() : "Chờ khám";
                    model.addRow(new Object[]{regCol, stt, status, "", "", "", ""});
                }
            }
        }
        statusPanel.setRoomSummaries(snapshot.rooms());
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    /** Hàm `clearQueueAndRoomSummaries`: xóa hangdoi va phòng `Summaries`. */
    private void clearQueueAndRoomSummaries() {
        statusPanel.setQueue(List.of());
        statusPanel.setRoomSummaries(List.of());
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    /** Hàm `updateActiveRooms`: cập nhật hoạt động phòng. */
    void updateActiveRooms(List<PhongKham> rooms) {
        List<PhongKham> safeRooms = rooms != null ? rooms : List.of();
        runOnEdt(() -> formPanel.setClinicRooms(safeRooms));
    }

    /** Hàm `currentAuthorizationContext`: hientai phanquyen ngữ cảnh. */
    private PhanQuyenNguCanh currentAuthorizationContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuthorizationContext().orElse(null);
        }
        NguoiDungPhien session = NguoiDungPhien.getInstance();
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
        return new PhanQuyenNguCanh(employeeId, roleId, roleCode, facilityId, departmentId, sessionId);
    }

    /** Hàm `currentAuditActorContext`: hientai kiểm toán tacnhan ngữ cảnh. */
    private KiemToanTacNhanNguCanh currentAuditActorContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuditActorContext().orElse(null);
        }
        NguoiDungPhien session = NguoiDungPhien.getInstance();
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
        return new KiemToanTacNhanNguCanh(employeeId, roleId, roleCode, facilityId, departmentId, workstationId, UUID.randomUUID());
    }

    /** Hàm `freshAuditContext`: moi kiểm toán ngữ cảnh. */
    private static KiemToanTacNhanNguCanh freshAuditContext(KiemToanTacNhanNguCanh context) {
        return new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                context.facilityId(), context.departmentId(), context.workstationId(), UUID.randomUUID());
    }

    /** Hàm `isCurrentWorker`: là hientai tacvunen. */
    private boolean isCurrentWorker(long epoch) {
        if (sessionLifecycleService != null
                && sessionLifecycleService.evaluate() != PhienVongDoiDichVu.State.ACTIVE) return false;
        return !disposed && viewSessionEpoch.get() == epoch;
    }

    public void destroy() {
        disposed = true;
        viewSessionEpoch.incrementAndGet();
        cancelInFlightWorkers();
        if (sessionLifecycleService != null && sessionListener != null) {
            sessionLifecycleService.removeSessionListener(sessionListener);
        }
    }

    /** Hàm `runOnEdt`: `run` tải `Edt`. */
    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /** Hàm `findComponents`: tìm `Components`. */
    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> findComponents(Component root, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        findComponentsRecursive(root, clazz, result);
        return result;
    }

    /** Hàm `findComponentsRecursive`: tìm `Components` `Recursive`. */
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

    /** Hàm `findButtonByText`: tìm nút theo văn bản. */
    private static Optional<JButton> findButtonByText(Container container, String textSubstring) {
        List<JButton> buttons = findComponents(container, JButton.class);
        for (JButton b : buttons) {
            if (b.getText() != null && b.getText().contains(textSubstring)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /** Hàm `getCompositionRoot`: lấy ghepnoi goc. */
    public TiepDonGhepNoiGoc getCompositionRoot() {
        return compositionRoot;
    }

    /** Hàm `getQueueHandler`: lấy hangdoi xử lý. */
    public GetTiepDonHangDoiHandler getQueueHandler() {
        return queueHandler;
    }

    /** Hàm `getRoomsHandler`: lấy phòng xử lý. */
    public GetHoatDongTiepDonPhongHandler getRoomsHandler() {
        return roomsHandler;
    }

    /** Hàm `getRegisterPatientHandler`: lấy đăng ký bệnh nhân xử lý. */
    public RegisterBenhNhanHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    /** Hàm `getSearchPatientsHandler`: lấy tìm kiếm bệnh nhân xử lý. */
    public SearchBenhNhanHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    /** Hàm `getSessionLifecycleService`: lấy phien vongdoi dịch vụ. */
    public PhienVongDoiDichVu getSessionLifecycleService() {
        return sessionLifecycleService;
    }

    /** Hàm `getFormPanel`: lấy bieumau màn hình. */
    public BenhNhanDangKyBieuMauManHinh getFormPanel() {
        return formPanel;
    }

    /** Hàm `getStatusPanel`: lấy trạng thái màn hình. */
    public PhongKhamPhongTrangThaiManHinh getStatusPanel() {
        return statusPanel;
    }

    /** Hàm `getTopToolbar`: lấy tren thanhcongcu. */
    public TiepDonTrenThanhCongCu getTopToolbar() {
        return topToolbar;
    }

    /** Hàm `saveRegistration`: lưu đăng ký. */
    public void saveRegistration() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::saveRegistration);
            return;
        }
        if (disposed) return;
        if (sessionLifecycleService != null
                && sessionLifecycleService.evaluate() != PhienVongDoiDichVu.State.ACTIVE) {
            showError("Phiên làm việc không hợp lệ hoặc đã hết hạn.");
            return;
        }
        if (registerPatientHandler == null) return;
        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null || auditContext == null) {
            showError("Phiên làm việc không hợp lệ hoặc đã hết hạn.");
            return;
        }

        final RegisterBenhNhanCommand command;
        try {
            command = formPanel.toRegisterCommand(authzContext, auditContext);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION"));
            return;
        }

        if (commandConsumerForTest != null) {
            commandConsumerForTest.accept(command);
        }

        String submittedCccd = formPanel.enteredCccd();
        long epoch = viewSessionEpoch.get();
        footerBar.setSaving(true);
        setComponentsEditable(formPanel, false);
        SwingWorker<KetQua<DangKyBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<DangKyBienNhan> doInBackground() {
                return registerPatientHandler.handle(command);
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (!isCurrentWorker(epoch)) return;
                footerBar.setSaving(false);
                setComponentsEditable(formPanel, sessionLifecycleService == null
                        || sessionLifecycleService.evaluate() == PhienVongDoiDichVu.State.ACTIVE);
                if (isCancelled()) return;
                try {
                    KetQua<DangKyBienNhan> result = get();
                    if (result.isFailure()) {
                        String errCode = result.getError().map(KetQua.ApplicationError::code).orElse("DEFAULT");
                        if ("PATIENT_DUPLICATE_CCCD".equals(errCode) || "PATIENT_IDENTITY_CONFLICT".equals(errCode)) {
                            handleDuplicateCccd(submittedCccd);
                        } else {
                            showError(TiepDonManHinhThongBaoChinhSach.toUserMessage(result));
                        }
                        return;
                    }
                    DangKyBienNhan receipt = result.getValue().orElseThrow();
                    showMessage("Đã đăng ký người bệnh " + receipt.patientCode() + " · Số tiếp đón " + receipt.queueNumberDisplay(),
                            "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                    if (!isCurrentWorker(epoch)) return;
                    clearForm();
                    refreshData();
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Safe cancellation
                } catch (ExecutionException ex) {
                    showError("Không thể lưu đăng ký. Vui lòng thử lại.");
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    /** Hàm `searchPatients`: tìm kiếm bệnh nhân. */
    public void searchPatients(String phone, String cccd) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> searchPatients(phone, cccd));
            return;
        }
        if (disposed) return;
        if (sessionLifecycleService != null
                && sessionLifecycleService.evaluate() != PhienVongDoiDichVu.State.ACTIVE) {
            showError("Phiên làm việc không hợp lệ hoặc đã hết hạn.");
            return;
        }
        if (searchPatientsHandler == null) return;
        String first = phone == null ? "" : phone.trim();
        String second = cccd == null ? "" : cccd.trim();
        if (first.isBlank() && second.isBlank()) {
            showError("Nhập số điện thoại hoặc CCCD để tìm hồ sơ.");
            return;
        }

        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null || auditContext == null) {
            showError("Phiên làm việc không hợp lệ hoặc đã hết hạn.");
            return;
        }

        long epoch = viewSessionEpoch.get();
        long sequence = searchSequence.incrementAndGet();
        SwingWorker<KetQua<List<BenhNhanSearchMatHang>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<BenhNhanSearchMatHang>> doInBackground() {
                Map<Integer, BenhNhanSearchMatHang> results = new LinkedHashMap<>();
                if (!first.isBlank() && !isCancelled()) {
                    KetQua<List<BenhNhanSearchMatHang>> r1 = searchPatientsHandler.handle(
                            new SearchBenhNhanQuery(first, authzContext, freshAuditContext(auditContext)));
                    if (r1.isFailure()) return r1;
                    r1.getValue().ifPresent(items -> items.forEach(it -> results.put(it.patient().getId(), it)));
                }
                if (!second.isBlank() && !second.equals(first) && !isCancelled()) {
                    KetQua<List<BenhNhanSearchMatHang>> r2 = searchPatientsHandler.handle(
                            new SearchBenhNhanQuery(second, authzContext, freshAuditContext(auditContext)));
                    if (r2.isFailure()) return r2;
                    r2.getValue().ifPresent(items -> items.forEach(it -> results.put(it.patient().getId(), it)));
                }
                return KetQua.success(results.values().stream().limit(50).toList());
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (!isCurrentWorker(epoch) || sequence != searchSequence.get() || isCancelled()) return;
                try {
                    KetQua<List<BenhNhanSearchMatHang>> result = get();
                    if (result.isFailure()) {
                        showError(TiepDonManHinhThongBaoChinhSach.toUserMessage(result));
                        return;
                    }
                    List<BenhNhanSearchMatHang> matches = result.getValue().orElse(List.of());
                    if (matches.isEmpty()) {
                        showMessage("Không tìm thấy hồ sơ phù hợp. Dữ liệu đang nhập vẫn được giữ nguyên.",
                                "Tìm người bệnh", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    BenhNhanSearchMatHang selected = matches.size() == 1 ? matches.get(0) : choosePatient(matches);
                    if (selected != null && isCurrentWorker(epoch) && sequence == searchSequence.get()) {
                        formPanel.selectPatient(selected.patient(), selected.latestInsurance());
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Safe cancellation
                } catch (ExecutionException ex) {
                    showError("Không thể tìm hồ sơ người bệnh. Kiểm tra kết nối cơ sở dữ liệu.");
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    /** Hàm `handleDuplicateCccd`: `handle` trunglap cccd. */
    private void handleDuplicateCccd(String cccd) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String msg = TiepDonManHinhThongBaoChinhSach.toDuplicateCccdMessage(cccd);
        int choice = JOptionPane.showConfirmDialog(this, msg, "Trùng số CCCD", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            formPanel.prepareDuplicateCccdSearch();
            searchPatients("", cccd);
        }
    }

    /** Hàm `choosePatient`: `choose` bệnh nhân. */
    private BenhNhanSearchMatHang choosePatient(List<BenhNhanSearchMatHang> matches) {
        if (GraphicsEnvironment.isHeadless()) {
            return matches.isEmpty() ? null : matches.get(0);
        }
        JOptionPane pane = new JOptionPane("Chọn hồ sơ người bệnh hiện có (Hủy để giữ thông tin đang nhập):",
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        pane.setWantsInput(true);
        pane.setSelectionValues(matches.toArray());
        pane.setInitialSelectionValue(matches.get(0));
        JDialog dialog = pane.createDialog(this, "Kết quả tìm kiếm");
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        return pane.getInputValue() instanceof BenhNhanSearchMatHang item ? item : null;
    }

    /** Hàm `showMessage`: `show` thông báo. */
    private void showMessage(String message, String title, int type) {
        if (type == JOptionPane.ERROR_MESSAGE) {
            this.lastErrorMessage = message;
            if (errorListener != null) {
                errorListener.accept(message);
            }
        } else {
            this.lastInfoMessage = message;
        }
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    /** Hàm `showError`: `show` lỗi. */
    private void showError(String message) {
        showMessage(message, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Hàm `setErrorListenerForTest`: gán lỗi bolangnghe cho test. */
    public void setErrorListenerForTest(java.util.function.Consumer<String> listener) {
        this.errorListener = listener;
    }

    /** Hàm `setCommandConsumerForTest`: gán lenh `Consumer` cho test. */
    public void setCommandConsumerForTest(java.util.function.Consumer<RegisterBenhNhanCommand> consumer) {
        this.commandConsumerForTest = consumer;
    }

    /** Hàm `getLastErrorMessage`: lấy `Last` lỗi thông báo. */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /** Hàm `getLastInfoMessage`: lấy `Last` `Info` thông báo. */
    public String getLastInfoMessage() {
        return lastInfoMessage;
    }

    /** Hàm `clearLastMessagesForTest`: xóa `Last` `Messages` cho test. */
    public void clearLastMessagesForTest() {
        this.lastErrorMessage = null;
        this.lastInfoMessage = null;
    }

    /** Hàm `getFooterBar`: lấy chantrang thanh. */
    public TiepDonChanTrangThanh getFooterBar() {
        return footerBar;
    }

    private record RefreshDataSnapshot(
            /** Trường `queueResult`: hangdoi kết quả. */
            KetQua<TiepDonHangDoiAnhChup> queueResult,
            /** Trường `roomsResult`: phòng kết quả. */
            KetQua<List<PhongKham>> roomsResult
    ) {}
}

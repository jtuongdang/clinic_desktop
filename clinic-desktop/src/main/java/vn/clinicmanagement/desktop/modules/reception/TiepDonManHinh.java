package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.desktop.modules.reception.components.TiepDonTrenThanhCongCu;

import vn.clinicmanagement.desktop.modules.reception.components.TiepDonChanTrangThanh;

import vn.clinicmanagement.desktop.modules.reception.components.BenhNhanDangKyBieuMauManHinh;

import vn.clinicmanagement.desktop.modules.reception.components.YTeBatLenBoChon;

import vn.clinicmanagement.desktop.modules.reception.components.PhongKhamPhongTrangThaiManHinh;

import vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamHandler;
import vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamQuery;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.get.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.get.GetHoatDongTiepDonPhongQuery;
import vn.clinicmanagement.application.reception.get.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.get.GetTiepDonHangDoiQuery;
import vn.clinicmanagement.application.reception.BenhNhanSearchMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiAnhChup;
import vn.clinicmanagement.application.reception.TiepDonLichSuKhamMatHang;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanCommand;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanHandler;
import vn.clinicmanagement.application.reception.DangKyBienNhan;
import vn.clinicmanagement.application.reception.search.SearchBenhNhanHandler;
import vn.clinicmanagement.application.reception.search.SearchBenhNhanQuery;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

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
    private final GetPhieuDangKyKhamHandler printSlipHandler;
    private Integer lastRegisteredVisitId;
    private boolean printingPreview;
    private boolean registrationSaving;
    private JDialog printPreviewDialog;


    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TiepDonTrenThanhCongCu topToolbar;
    private final BenhNhanDangKyBieuMauManHinh formPanel;
    private final PhongKhamPhongTrangThaiManHinh statusPanel;
    private final TiepDonChanTrangThanh footerBar;

    private final TiepDonGhepNoiGoc compositionRoot;
    private final GetTiepDonHangDoiHandler queueHandler;
    private final GetHoatDongTiepDonPhongHandler roomsHandler;
    private final RegisterBenhNhanHandler registerPatientHandler;
    private final SearchBenhNhanHandler searchPatientsHandler;
    private final IChiDinhDichVuRepository serviceRepository;
    private final PhienVongDoiDichVu sessionLifecycleService;
    private final PhienVongDoiDichVu.SessionListener sessionListener;

    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    private final AtomicLong viewSessionEpoch = new AtomicLong();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong searchSequence = new AtomicLong();
    private volatile boolean disposed;
    private Clock queueClock = Clock.system(CLINIC_ZONE);

    // Các điểm móc kiểm thử không cần giao diện.
    private volatile String lastErrorMessage;
    private volatile String lastInfoMessage;
    private java.util.function.Consumer<String> errorListener;
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
        this.printSlipHandler = compositionRoot == null ? null : compositionRoot.getPrintSlipHandler();
        this.queueHandler = queueHandler;
        this.roomsHandler = roomsHandler;
        this.registerPatientHandler = registerPatientHandler;
        this.searchPatientsHandler = searchPatientsHandler;
        this.serviceRepository = compositionRoot != null ? compositionRoot.getServiceRepository() : null;
        this.sessionLifecycleService = sessionLifecycleService;

        // 1. Thanh công cụ đỉnh
        topToolbar = new TiepDonTrenThanhCongCu();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Vùng trung tâm: Chia dọc (Trên: Form đăng ký; Dưới: Trạng thái phòng khám)
        formPanel = new BenhNhanDangKyBieuMauManHinh();
        statusPanel = new PhongKhamPhongTrangThaiManHinh();

        // Đồng bộ khi chọn phòng khám trên form -> Đổi màu đỏ phòng trong bảng trạng thái
        formPanel.setOnClinicRoomChanged(room -> statusPanel.setSelectedClinicRoom(room));

        // Form cao đúng 216 đơn vị Swing theo mẫu; phần bảng nhận diện tích còn lại.
        JPanel content = new JPanel(new BorderLayout());
        formPanel.setPreferredSize(new Dimension(1392, 216));
        content.add(formPanel, BorderLayout.NORTH);
        content.add(statusPanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // 3. Thanh phím tắt chân trang
        footerBar = new TiepDonChanTrangThanh();
        add(footerBar, BorderLayout.SOUTH);

        // Gắn các nút thanh công cụ và bộ lắng nghe thao tác.
        topToolbar.setOnRefresh(this::refreshData);
        topToolbar.setOnNew(this::clearForm);
        footerBar.setOnSave(this::saveRegistration);
        footerBar.setOnPrint(this::openPrintSlip);
        topToolbar.setOnPrint(this::openPrintSlip);
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

    private void handleSessionChanged() {
        if (sessionLifecycleService == null) return;
        closePrintPreview();
        lastRegisteredVisitId = null;
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

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void onHide() {
        closePrintPreview();
        viewSessionEpoch.incrementAndGet();
        cancelInFlightWorkers();
    }

    void setQueueClock(Clock clock) {
        queueClock = clock.withZone(CLINIC_ZONE);
    }

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
        if (serviceRepository != null) formPanel.beginClinicalServiceLoad();

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
                List<DanhMucDichVu> servicesResult = null;
                if (serviceRepository != null) {
                    try {
                        servicesResult = serviceRepository.searchDichVu("", LoaiDichVu.KHAM);
                    } catch (Exception ex) {
                        // Giữ null để phía giao diện xóa danh mục cũ và thông báo không thể đăng ký.
                    }
                }
                return new RefreshDataSnapshot(queueResult, roomsResult, servicesResult);
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
                        if (serviceRepository != null) TiepDonManHinh.this.updateClinicalServices(snapshot.servicesResult == null ? List.of() : snapshot.servicesResult);
                        if (serviceRepository != null && (snapshot.servicesResult == null || snapshot.servicesResult.isEmpty()))
                            showError(snapshot.servicesResult == null
                                    ? "Không thể tải danh mục dịch vụ khám. Vui lòng bấm Làm Mới để thử lại."
                                    : "Danh mục chưa có dịch vụ khám đang hoạt động. Vui lòng kiểm tra cấu hình dịch vụ trước khi đăng ký.");
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Hủy an toàn khi khóa phiên hoặc đóng cửa sổ.
                } catch (ExecutionException ex) {
                    TiepDonManHinh.this.clearQueueAndRoomSummaries();
                    TiepDonManHinh.this.updateActiveRooms(List.of());
                    TiepDonManHinh.this.updateClinicalServices(List.of());
                    showError(TiepDonManHinhThongBaoChinhSach.MSG_RECEPTION_QUEUE_FAILED);
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
        closePrintPreview();
        viewSessionEpoch.incrementAndGet();
        lastRegisteredVisitId = null;
        registrationSaving = false;
        statusPanel.getQueueTable().clearSelection();
        formPanel.clearForm();
        statusPanel.setPatientHistory(List.of());
        footerBar.setSaving(false);
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

    private void updateQueueAndRoomSummaries(TiepDonHangDoiAnhChup snapshot) {
        if (snapshot == null) return;
        // Cập nhật dữ liệu hiển thị và ánh xạ ID lượt khám cùng lúc để chọn đúng phiếu.
        statusPanel.setQueue(snapshot.queue() == null ? List.of() : snapshot.queue());
        statusPanel.setRoomSummaries(snapshot.rooms());
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    public void loadPatientVisitHistory(Integer patientId) {
        if (patientId == null || patientId <= 0 || compositionRoot == null
                || compositionRoot.getQueueRepository() == null) {
            runOnEdt(() -> statusPanel.setPatientHistory(List.of()));
            return;
        }
        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        int facilityId = authzContext != null ? authzContext.facilityId() : 1;
        int departmentId = authzContext != null ? authzContext.departmentId() : 1;
        long epoch = viewSessionEpoch.get();

        SwingWorker<List<TiepDonLichSuKhamMatHang>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TiepDonLichSuKhamMatHang> doInBackground() {
                return compositionRoot.getQueueRepository()
                        .getPatientVisitHistory(patientId, facilityId, departmentId);
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (!isCurrentWorker(epoch) || isCancelled()) return;
                try {
                    List<TiepDonLichSuKhamMatHang> history = get();
                    statusPanel.setPatientHistory(history != null ? history : List.of());
                } catch (Exception ignored) {
                    statusPanel.setPatientHistory(List.of());
                }
            }
        };
        inFlightWorkers.add(worker);
        worker.execute();
    }

    private void clearQueueAndRoomSummaries() {
        statusPanel.setQueue(List.of());
        statusPanel.setRoomSummaries(List.of());
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    void updateActiveRooms(List<PhongKham> rooms) {
        List<PhongKham> safeRooms = rooms != null ? rooms : List.of();
        runOnEdt(() -> formPanel.setClinicRooms(safeRooms));
    }

    void updateClinicalServices(List<DanhMucDichVu> services) {
        List<DanhMucDichVu> safeServices = services != null ? services : List.of();
        runOnEdt(() -> formPanel.setClinicalServices(safeServices));
    }

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

    private static KiemToanTacNhanNguCanh freshAuditContext(KiemToanTacNhanNguCanh context) {
        return new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                context.facilityId(), context.departmentId(), context.workstationId(), UUID.randomUUID());
    }

    private boolean isCurrentWorker(long epoch) {
        if (sessionLifecycleService != null
                && sessionLifecycleService.evaluate() != PhienVongDoiDichVu.State.ACTIVE) return false;
        return !disposed && viewSessionEpoch.get() == epoch;
    }

    public void destroy() {
        closePrintPreview();
        disposed = true;
        viewSessionEpoch.incrementAndGet();
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

    public TiepDonGhepNoiGoc getCompositionRoot() {
        return compositionRoot;
    }

    public GetTiepDonHangDoiHandler getQueueHandler() {
        return queueHandler;
    }

    public GetHoatDongTiepDonPhongHandler getRoomsHandler() {
        return roomsHandler;
    }

    public RegisterBenhNhanHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    public SearchBenhNhanHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    public PhienVongDoiDichVu getSessionLifecycleService() {
        return sessionLifecycleService;
    }

    public BenhNhanDangKyBieuMauManHinh getFormPanel() {
        return formPanel;
    }

    public PhongKhamPhongTrangThaiManHinh getStatusPanel() {
        return statusPanel;
    }

    public TiepDonTrenThanhCongCu getTopToolbar() {
        return topToolbar;
    }

    /** Đọc lại phiếu từ DB cho lượt vừa lưu hoặc lượt được chọn, có kiểm tra phiên và loại bỏ kết quả cũ. */
    public void openPrintSlip() {
        if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::openPrintSlip); return; }
        if (disposed || printingPreview || registrationSaving) return;
        if (!printSessionActive()) { showError("Phiên làm việc không hợp lệ hoặc đã hết hạn."); return; }
        if (printPreviewDialog != null && printPreviewDialog.isDisplayable()) { printPreviewDialog.toFront(); return; }
        Integer selectedVisit = statusPanel.getSelectedVisitId();
        Integer visit = selectedVisit != null ? selectedVisit : lastRegisteredVisitId;
        if (visit == null) { showError("Vui lòng lưu hoặc chọn lượt đăng ký trước khi in phiếu."); return; }
        if (printSlipHandler == null) { showError("Chưa kết nối dịch vụ đọc phiếu."); return; }
        var authorization = currentAuthorizationContext();
        var actor = currentAuditActorContext();
        if (authorization == null || actor == null) { showError("Phiên làm việc không hợp lệ hoặc đã hết hạn."); return; }
        long epoch = viewSessionEpoch.get(); printingPreview = true;
        SwingWorker<KetQua<vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu>, Void> worker = new SwingWorker<>() {
            @Override protected KetQua<vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu> doInBackground() {
                return printSlipHandler.handle(new GetPhieuDangKyKhamQuery(visit, authorization, actor));
            }
            @Override protected void done() {
                inFlightWorkers.remove(this); printingPreview = false;
                if (!isCurrentWorker(epoch) || isCancelled() || !printSessionActive()) return;
                try {
                    var result = get();
                    if (result.isFailure()) { showError(result.getError().map(KetQua.ApplicationError::message).orElse("Không thể tải phiếu.")); return; }
                    if (!GraphicsEnvironment.isHeadless()) printPreviewDialog = vn.clinicmanagement.desktop.modules.reception.print.PhieuDangKyKhamManHinh.open(
                            SwingUtilities.getWindowAncestor(TiepDonManHinh.this), result.getValue().orElseThrow(),
                            () -> !disposed && viewSessionEpoch.get() == epoch && printSessionActive(),
                            action -> printSlipHandler.handle(new GetPhieuDangKyKhamQuery(visit, authorization, freshAuditActor(actor), action)).isSuccess());
                } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (CancellationException ignored) { }
                catch (ExecutionException error) { showError("Không thể tải phiếu từ cơ sở dữ liệu."); }
            }
        };
        inFlightWorkers.add(worker); worker.execute();
    }

    private static KiemToanTacNhanNguCanh freshAuditActor(KiemToanTacNhanNguCanh actor) {
        return new KiemToanTacNhanNguCanh(actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(),
                actor.departmentId(), actor.workstationId(), java.util.UUID.randomUUID());
    }
    private boolean printSessionActive() {
        return sessionLifecycleService == null || sessionLifecycleService.evaluate() == PhienVongDoiDichVu.State.ACTIVE;
    }
    private void closePrintPreview() {
        if (printPreviewDialog != null) { printPreviewDialog.dispose(); printPreviewDialog = null; }
    }

    public void saveRegistration() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::saveRegistration);
            return;
        }
        if (disposed || registrationSaving) return;
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
        } catch (vn.clinicmanagement.desktop.modules.reception.components.DangKyKhamNhapLieuNgoaiLe ex) {
            showError(ex.getMessage());
            ex.focusInput();
            return;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(TiepDonManHinhThongBaoChinhSach.toUserMessage("PATIENT_VALIDATION"));
            return;
        }

        if (commandConsumerForTest != null) {
            commandConsumerForTest.accept(command);
        }

        String submittedCccd = formPanel.enteredCccd();
        long epoch = viewSessionEpoch.get();
        registrationSaving = true;
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
                registrationSaving = false;
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
                            if ("SERVICE_UNAVAILABLE".equals(errCode) || "SERVICE_CATALOG_FAILED".equals(errCode))
                                new vn.clinicmanagement.desktop.modules.reception.components.DangKyKhamNhapLieuNgoaiLe(
                                        "Vui lòng kiểm tra lại dịch vụ khám.", formPanel.getChooserService().getTextField()).focusInput();
                        }
                        return;
                    }
                    DangKyBienNhan receipt = result.getValue().orElseThrow();
                    showMessage("Đã đăng ký người bệnh " + receipt.patientCode() + " · Số tiếp đón " + receipt.queueNumberDisplay(),
                            "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                    if (!isCurrentWorker(epoch)) return;
                    clearForm();
                    lastRegisteredVisitId = receipt.visitId();
                    refreshData();
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Hủy an toàn.
                } catch (ExecutionException ex) {
                    showError("Không thể lưu đăng ký. Vui lòng thử lại.");
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

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
                        lastRegisteredVisitId = null;
                        statusPanel.getQueueTable().clearSelection();
                        formPanel.selectPatient(selected.patient(), selected.latestInsurance());
                        loadPatientVisitHistory(selected.patient().getId());
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Hủy an toàn.
                } catch (ExecutionException ex) {
                    showError("Không thể tìm hồ sơ người bệnh. Kiểm tra kết nối cơ sở dữ liệu.");
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

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

    private void showError(String message) {
        showMessage(message, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public void setErrorListenerForTest(java.util.function.Consumer<String> listener) {
        this.errorListener = listener;
    }

    public void setCommandConsumerForTest(java.util.function.Consumer<RegisterBenhNhanCommand> consumer) {
        this.commandConsumerForTest = consumer;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getLastInfoMessage() {
        return lastInfoMessage;
    }

    public void clearLastMessagesForTest() {
        this.lastErrorMessage = null;
        this.lastInfoMessage = null;
    }

    public TiepDonChanTrangThanh getFooterBar() {
        return footerBar;
    }

    public IChiDinhDichVuRepository getServiceRepository() {
        return serviceRepository;
    }

    private record RefreshDataSnapshot(
            KetQua<TiepDonHangDoiAnhChup> queueResult,
            KetQua<List<PhongKham>> roomsResult,
            List<DanhMucDichVu> servicesResult
    ) {}
}

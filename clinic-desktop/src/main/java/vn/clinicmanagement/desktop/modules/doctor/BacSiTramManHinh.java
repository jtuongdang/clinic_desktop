package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.desktop.modules.doctor.model.BenhNhanKhamDuLieu;

import vn.clinicmanagement.desktop.modules.doctor.components.BacSiHangDoiThanhBen;

import vn.clinicmanagement.desktop.modules.doctor.components.BacSiTienSuManHinh;

import vn.clinicmanagement.desktop.modules.doctor.components.BacSiLamSangBieuMau;

import vn.clinicmanagement.desktop.modules.doctor.components.BacSiHanhDongThanh;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.get.GetBacSiHangDoiQuery;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

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
public class BacSiTramManHinh extends CoSoManHinhManHinh {

    private final List<BenhNhanKhamDuLieu> patientList = new ArrayList<>();
    private final BacSiGhepNoiGoc.Services services;
    private final PhienVongDoiDichVu sessionLifecycleService;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong viewSessionEpoch = new AtomicLong();

    private BacSiHanhDongThanh actionBar;
    private BacSiHangDoiThanhBen queueSidebar;
    private BacSiLamSangBieuMau clinicalForm;
    private BacSiTienSuManHinh historyPanel;
    private JTabbedPane clinicalTabs;
    private BenhNhanKhamDuLieu currentPatient;

    public BacSiTramManHinh() {
        this(resolveDefaultServices(), null);
    }

    public BacSiTramManHinh(BacSiGhepNoiGoc.Services services) {
        this(services, null);
    }

    public BacSiTramManHinh(BacSiGhepNoiGoc.Services services, PhienVongDoiDichVu sessionLifecycleService) {
        super("treatment", "Điều Trị", "👨‍⚕️");
        this.services = services;
        this.sessionLifecycleService = sessionLifecycleService;
        buildUI();
    }

    private static BacSiGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return BacSiGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Top Action Bar + Banner tóm tắt bệnh nhân
        actionBar = new BacSiHanhDongThanh();
        actionBar.setOnActionClickListener(this::handleDoctorAction);
        add(actionBar, BorderLayout.NORTH);

        // 2. Main Split: Cột trái (Hàng đợi) + Cột phải (Hồ sơ khám)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);

        // Cột trái
        queueSidebar = new BacSiHangDoiThanhBen(patientList);
        queueSidebar.setOnPatientSelectedListener(this::onPatientChanged);
        mainSplit.setLeftComponent(queueSidebar);

        // Cột phải
        JPanel rightWorkPanel = new JPanel(new BorderLayout());
        rightWorkPanel.setBackground(Color.WHITE);

        clinicalTabs = new JTabbedPane();
        clinicalTabs.setFont(UngDungPhongChu.BODY_BOLD);

        // Tab "Bệnh Nhân"
        JPanel patientTab = new JPanel(new BorderLayout(0, 4));
        patientTab.setBackground(Color.WHITE);
        patientTab.setBorder(new EmptyBorder(8, 12, 8, 12));

        clinicalForm = new BacSiLamSangBieuMau();
        patientTab.add(clinicalForm, BorderLayout.NORTH);

        historyPanel = new BacSiTienSuManHinh();
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

    private void onPatientChanged(BenhNhanKhamDuLieu p) {
        this.currentPatient = p;
        actionBar.updatePatient(p);
        clinicalForm.updatePatient(p);
        historyPanel.updateHistory(p);
    }

    private void handleDoctorAction(String action) {
        if (action == null) return;
        switch (action) {
            case "Làm mới" -> refreshData();
            case "Bệnh Án" -> {
                if (clinicalTabs != null && clinicalTabs.getTabCount() > 0) {
                    clinicalTabs.setSelectedIndex(0);
                }
            }
            case "Dịch Vụ" -> {
                if (clinicalTabs != null && clinicalTabs.getTabCount() > 1) {
                    clinicalTabs.setSelectedIndex(1);
                }
            }
            case "Thuốc/VT" -> {
                if (clinicalTabs != null && clinicalTabs.getTabCount() > 2) {
                    clinicalTabs.setSelectedIndex(2);
                }
            }
            case "Phác Đồ ĐT" -> {
                if (clinicalTabs != null && clinicalTabs.getTabCount() > 3) {
                    clinicalTabs.setSelectedIndex(3);
                }
            }
            case "Thanh Toán" -> {
                if (clinicalTabs != null && clinicalTabs.getTabCount() > 4) {
                    clinicalTabs.setSelectedIndex(4);
                }
            }
            case "Tìm Kiếm" -> {
                if (queueSidebar != null) {
                    queueSidebar.requestFocusInWindow();
                }
            }
            case "In" -> printPatientSummary();
            case "Menu" -> {
                if (isShowing() && !java.awt.GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this,
                            "Menu tác vụ mở rộng của Bác sĩ đang hoạt động.",
                            "Tác Vụ Bác Sĩ",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
            default -> {}
        }
    }

    public void printPatientSummary() {
        if (currentPatient == null) {
            if (isShowing() && !java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn một bệnh nhân trong hàng đợi để in hồ sơ.",
                        "Thông Báo",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if (isShowing() && !java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Đang gửi lệnh in hồ sơ tóm tắt cho bệnh nhân:\n"
                            + currentPatient.getName() + " (" + currentPatient.getCode() + ")",
                    "In Hồ Sơ Tóm Tắt",
                    JOptionPane.INFORMATION_MESSAGE);
        }
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

        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null) {
            queueSidebar.setPatients(List.of());
            onPatientChanged(null);
            return;
        }

        long epoch = viewSessionEpoch.get();
        long sequence = refreshSequence.incrementAndGet();

        SwingWorker<KetQua<List<TiepDonHangDoiMatHang>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<TiepDonHangDoiMatHang>> doInBackground() {
                GetBacSiHangDoiQuery query = new GetBacSiHangDoiQuery(
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
                    KetQua<List<TiepDonHangDoiMatHang>> result = get();
                    if (result != null && result.isSuccess()) {
                        List<TiepDonHangDoiMatHang> items = result.getValue().orElse(List.of());
                        List<BenhNhanKhamDuLieu> mapped = new ArrayList<>();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                        for (TiepDonHangDoiMatHang item : items) {
                            if (item == null) continue;
                            String regTime = item.registeredAt() != null
                                    ? item.registeredAt().format(formatter)
                                    : "";
                            mapped.add(new BenhNhanKhamDuLieu(
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
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
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

    public BacSiGhepNoiGoc.Services getServices() {
        return services;
    }

    public BacSiHangDoiThanhBen getQueueSidebar() {
        return queueSidebar;
    }

    public BacSiHanhDongThanh getActionBar() {
        return actionBar;
    }

    public BacSiLamSangBieuMau getClinicalForm() {
        return clinicalForm;
    }

    public BacSiTienSuManHinh getHistoryPanel() {
        return historyPanel;
    }

    public JTabbedPane getClinicalTabs() {
        return clinicalTabs;
    }

    private PhanQuyenNguCanh currentAuthorizationContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuthorizationContext().orElse(null);
        }
        NguoiDungPhien session = NguoiDungPhien.getInstance();
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
                ? session.getRoleCode() : "DOCTOR";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        String workstationId = session.getWorkstationId();
        if (workstationId == null || workstationId.isBlank()) {
            workstationId = "DOCTOR-01";
        }
        return new KiemToanTacNhanNguCanh(employeeId, roleId, roleCode, facilityId, departmentId, workstationId, UUID.randomUUID());
    }
}

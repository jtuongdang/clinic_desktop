package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.desktop.core.session.UserSession;
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
    private final ReceptionCompositionRoot.Services services;
    private boolean loading;

    public ReceptionPanel() {
        super("reception", "Đăng Ký Khám Bệnh", "📋");
        services = ReceptionCompositionRoot.create();
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
        refreshData();
    }

    @Override
    public void refreshData() {
        if (loading) return;
        loading = true;
        new SwingWorker<ReceptionLoad, Void>() {
            @Override protected ReceptionLoad doInBackground() {
                var rooms = services.rooms().getActiveRooms();
                ReceptionQueueSnapshot snapshot = services.getQueue().handle(new GetReceptionQueueQuery(java.time.LocalDate.now())).valueOrThrow();
                return new ReceptionLoad(rooms, snapshot);
            }
            @Override protected void done() {
                loading = false;
                try {
                    ReceptionLoad load = get();
                    formPanel.setClinicRooms(load.rooms());
                    ReceptionQueueSnapshot snapshot = load.snapshot();
                    statusPanel.setQueue(snapshot.queue());
                    statusPanel.setRoomSummaries(snapshot.rooms());
                    topToolbar.setNextQueueNumber(snapshot.queue().isEmpty() ? null : snapshot.queue().get(0).queueNumberDisplay());
                } catch (Exception ex) {
                    showError("Không thể tải phòng khám và hàng đợi. Kiểm tra kết nối cơ sở dữ liệu.");
                }
            }
        }.execute();
    }

    private void searchPatients(String phone, String cccd) {
        String first = phone == null ? "" : phone.trim();
        String second = cccd == null ? "" : cccd.trim();
        if (first.isBlank() && second.isBlank()) { showError("Nhập số điện thoại hoặc CCCD để tìm hồ sơ."); return; }
        new SwingWorker<java.util.List<PatientSearchItem>, Void>() {
            @Override protected java.util.List<PatientSearchItem> doInBackground() {
                Map<Integer, PatientSearchItem> results = new LinkedHashMap<>();
                if (!first.isBlank()) addMatches(results, first);
                if (!second.isBlank() && !second.equals(first)) addMatches(results, second);
                return new ArrayList<>(results.values());
            }
            private void addMatches(Map<Integer, PatientSearchItem> results, String keyword) {
                Result<java.util.List<PatientSearchItem>> result = services.searchPatients().handle(new SearchPatientsQuery(keyword));
                if (result.isSuccess()) result.getValue().orElseThrow().forEach(item -> results.put(item.patient().getId(), item));
                else throw new IllegalStateException(result.getError().orElseThrow().message());
            }
            @Override protected void done() {
                try {
                    var matches = get();
                    if (matches.isEmpty()) { JOptionPane.showMessageDialog(ReceptionPanel.this, "Không tìm thấy hồ sơ phù hợp.", "Tìm người bệnh", JOptionPane.INFORMATION_MESSAGE); return; }
                    PatientSearchItem selected;
                    if (matches.size() == 1) selected = matches.get(0);
                    else {
                        Object choice = JOptionPane.showInputDialog(ReceptionPanel.this, "Chọn hồ sơ người bệnh:", "Kết quả tìm kiếm", JOptionPane.PLAIN_MESSAGE, null, matches.toArray(), matches.get(0));
                        if (!(choice instanceof PatientSearchItem item)) return;
                        selected = item;
                    }
                    formPanel.selectPatient(selected.patient(), selected.latestInsurance());
                } catch (Exception ex) { showError("Không thể tìm hồ sơ người bệnh. Kiểm tra kết nối cơ sở dữ liệu."); }
            }
        }.execute();
    }

    private void saveRegistration() {
        final RegisterPatientCommand command;
        try { command = formPanel.toRegisterCommand(UserSession.getInstance().getUserId()); }
        catch (IllegalArgumentException ex) { showError(ex.getMessage()); return; }
        footerBar.setSaving(true);
        new SwingWorker<Result<RegistrationReceipt>, Void>() {
            @Override protected Result<RegistrationReceipt> doInBackground() { return services.registerPatient().handle(command); }
            @Override protected void done() {
                footerBar.setSaving(false);
                try {
                    Result<RegistrationReceipt> result = get();
                    if (result.isFailure()) { showError(result.getError().orElseThrow().message()); return; }
                    RegistrationReceipt receipt = result.getValue().orElseThrow();
                    JOptionPane.showMessageDialog(ReceptionPanel.this,
                            "Đã đăng ký người bệnh " + receipt.patientCode() + " · Số tiếp đón " + receipt.queueNumberDisplay(),
                            "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                    formPanel.clearForm();
                    refreshData();
                } catch (Exception ex) { showError("Không thể lưu đăng ký. Vui lòng thử lại."); }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Tiếp đón người bệnh", JOptionPane.ERROR_MESSAGE);
    }

    private record ReceptionLoad(java.util.List<PhongKham> rooms, ReceptionQueueSnapshot snapshot) {}
}

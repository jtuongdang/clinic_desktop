package vn.clinicmanagement.desktop.modules.examination.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.MedicalSuppliesOrderPanel;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenMedicalRecordPanel;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenMedicalRecordPanel.ReopenRecordRequest;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.SuppliesOrderTabPanel;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Tests: Supplies & Reopen Record Wiring")
class SuppliesAndReopenWiringTest {

    @BeforeAll
    static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private AdminPatientItem createTestPatient() {
        return new AdminPatientItem(
                "1",
                "NGUYỄN VĂN AN",
                "22001122",
                "Viện phí",
                "28 tuổi",
                false,
                "Nam",
                "1995",
                "28 tuổi",
                "28 tuổi",
                "15/05/1995",
                "123 Hoàng Quốc Việt, Cầu Giấy, Hà Nội",
                "Cầu Giấy, Hà Nội",
                "0987654321",
                "BA-9999",
                "08:30 15/10/2023",
                "PK Nội Tổng Hợp",
                "BS. Nguyễn Văn A",
                "K29 Viêm dạ dày tá tràng",
                "VP-105"
        );
    }

    @Test
    @DisplayName("SuppliesOrderTabPanel: Khởi tạo và thiết lập thông tin bệnh nhân chính xác")
    void testSuppliesOrderTabPanelPatientBinding() {
        AdminPatientItem patient = createTestPatient();
        SuppliesOrderTabPanel panel = new SuppliesOrderTabPanel(patient);
        panel.setSuppressDialogs(true);

        assertNotNull(panel.getTableModel());
        assertNotNull(panel.getSuppliesTable());
        assertEquals("K29 Viêm dạ dày tá tràng", panel.getLblChanDoan().getText());
        assertEquals(patient, panel.getPatient());

        // In phiếu an toàn
        assertDoesNotThrow(panel::executePrint);
    }

    @Test
    @DisplayName("SuppliesOrderTabPanel: Hủy phiếu cập nhật trạng thái và dòng bảng")
    void testSuppliesOrderTabPanelCancelFlow() {
        AdminPatientItem patient = createTestPatient();
        SuppliesOrderTabPanel panel = new SuppliesOrderTabPanel(patient);
        panel.setSuppressDialogs(true);

        assertFalse(panel.isCancelled());
        panel.executeCancel();

        assertTrue(panel.isCancelled());
        assertTrue(panel.getLblTrangThai().getText().contains("ĐÃ HỦY PHIẾU"));

        DefaultTableModel model = panel.getTableModel();
        String itemCol = (String) model.getValueAt(0, 2);
        assertTrue(itemCol.contains("[Đã hủy]"), "Dòng vật tư phải có nhãn [Đã hủy]");
    }

    @Test
    @DisplayName("MedicalSuppliesOrderPanel: Đấu nối dữ liệu thực tế và lưu phiếu chỉ định")
    void testMedicalSuppliesOrderPanelSave() {
        AdminPatientItem patient = createTestPatient();
        MedicalSuppliesOrderPanel orderPanel = new MedicalSuppliesOrderPanel();
        orderPanel.setSuppressDialogs(true);
        orderPanel.setPatient(patient);

        AtomicReference<List<Object[]>> savedRef = new AtomicReference<>();
        orderPanel.setOnSuppliesSavedCallback(savedRef::set);

        orderPanel.executeSave();

        assertNotNull(savedRef.get(), "Callback lưu vật tư phải được gọi");
        assertFalse(savedRef.get().isEmpty(), "Phải có danh sách vật tư đã lưu");

        // Kiểm tra vật tư đầu tiên
        Object[] first = savedRef.get().get(0);
        assertEquals("VT026", first[1]);
        assertTrue(first[2].toString().contains("Bơm cho ăn 5ml"));
    }

    @Test
    @DisplayName("Đấu nối end-to-end: Lưu từ MedicalSuppliesOrderPanel tự cập nhật vào SuppliesOrderTabPanel")
    void testSuppliesWiringEndToEnd() {
        AdminPatientItem patient = createTestPatient();
        SuppliesOrderTabPanel tabPanel = new SuppliesOrderTabPanel(patient);
        tabPanel.setSuppressDialogs(true);

        MedicalSuppliesOrderPanel orderPanel = new MedicalSuppliesOrderPanel();
        orderPanel.setSuppressDialogs(true);
        orderPanel.setPatient(patient);

        // Đấu nối callback giữa 2 panel giống như ExaminationPanel
        orderPanel.setOnSuppliesSavedCallback(tabPanel::setSupplies);

        // Thực hiện lưu
        orderPanel.executeSave();

        // Kiểm tra tab panel đã nhận dữ liệu mới
        assertFalse(tabPanel.isCancelled());
        DefaultTableModel tabModel = tabPanel.getTableModel();
        assertEquals("VT026", tabModel.getValueAt(0, 1));
        assertTrue(tabModel.getValueAt(0, 2).toString().contains("Bơm cho ăn 5ml"));
    }

    @Test
    @DisplayName("ReopenMedicalRecordPanel: Kiểm tra xác thực lý do và kích hoạt mở bệnh án")
    void testReopenMedicalRecordPanelFlow() {
        AdminPatientItem patient = createTestPatient();
        AtomicReference<ReopenRecordRequest> requestRef = new AtomicReference<>();
        AtomicBoolean closedRef = new AtomicBoolean(false);

        ReopenMedicalRecordPanel reopenPanel = new ReopenMedicalRecordPanel(
                patient,
                requestRef::set,
                () -> closedRef.set(true)
        );
        reopenPanel.setSuppressDialogs(true);

        // Trường hợp 1: Không nhập lý do -> không cho lưu
        reopenPanel.getTxtReason().setText("");
        reopenPanel.executeSave();
        assertNull(requestRef.get(), "Không được kích hoạt callback khi lý do trống");
        assertFalse(closedRef.get());

        // Trường hợp 2: Có lý do hợp lệ -> lưu thành công và đóng dialog
        reopenPanel.getTxtReason().setText("Bác sĩ yêu cầu bổ sung kết quả xét nghiệm");
        reopenPanel.executeSave();

        assertNotNull(requestRef.get(), "Callback phải nhận được request");
        assertEquals("22001122", requestRef.get().patientCode);
        assertEquals("NGUYỄN VĂN AN", requestRef.get().patientName);
        assertEquals("Bác sĩ yêu cầu bổ sung kết quả xét nghiệm", requestRef.get().reason);
        assertTrue(closedRef.get(), "Hộp thoại phải được đóng lại sau khi lưu thành công");
    }
}

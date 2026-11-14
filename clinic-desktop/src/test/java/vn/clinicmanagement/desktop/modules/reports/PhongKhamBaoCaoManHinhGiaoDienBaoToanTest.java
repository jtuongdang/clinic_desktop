package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableModel;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClinicReportPanel UI Preservation Tests")
class PhongKhamBaoCaoManHinhGiaoDienBaoToanTest {

    private PhongKhamBaoCaoManHinh panel;

    @BeforeEach
    void setUp() {
        panel = new PhongKhamBaoCaoManHinh();
    }

    @Test
    @DisplayName("Module identity and title are preserved")
    void moduleIdentityPreserved() {
        assertEquals("report-mgmt", panel.getViewKey());
        assertEquals("Báo Cáo Thống Kê", panel.getViewTitle());
        assertEquals("📊", panel.getIconEmoji());
    }

    @Test
    @DisplayName("7 table columns match legacy specifications exactly")
    void tableColumnsPreserved() {
        TableModel model = panel.getTableModel();
        assertNotNull(model);
        assertEquals(7, model.getColumnCount());
        assertEquals("STT", model.getColumnName(0));
        assertEquals("Chỉ Tiêu / Hạng Mục", model.getColumnName(1));
        assertEquals("Đơn Vị Tính", model.getColumnName(2));
        assertEquals("Kỳ Trước", model.getColumnName(3));
        assertEquals("Kỳ Này", model.getColumnName(4));
        assertEquals("Tăng/Giảm (%)", model.getColumnName(5));
        assertEquals("Ghi Chú", model.getColumnName(6));
    }

    @Test
    @DisplayName("Filter controls and buttons are instantiated correctly")
    void controlsArePreserved() {
        JComboBox<String> cbo = panel.getCboReportType();
        assertNotNull(cbo);
        assertEquals(5, cbo.getItemCount());
        assertEquals("Báo cáo tổng hợp lượt khám theo ngày", cbo.getItemAt(0));
        assertEquals("Báo cáo doanh thu viện phí theo khoa/phòng", cbo.getItemAt(1));
        assertEquals("Báo cáo xuất nhập tồn kho dược & vật tư", cbo.getItemAt(2));
        assertEquals("Báo cáo tỷ lệ bệnh tật theo nhóm ICD-10", cbo.getItemAt(3));
        assertEquals("Báo cáo hiệu suất phục vụ theo bác sĩ", cbo.getItemAt(4));

        assertNotNull(panel.getTxtFromDate());
        assertNotNull(panel.getTxtToDate());
        assertNotNull(panel.getBtnView());
        assertNotNull(panel.getBtnExport());
        assertNotNull(panel.getBtnPrint());

        assertEquals("🔍 Xem Báo Cáo", panel.getBtnView().getText());
        assertEquals("📥 Xuất Excel", panel.getBtnExport().getText());
        assertEquals("🖨️ In Báo Cáo", panel.getBtnPrint().getText());
    }

    @Test
    @DisplayName("Table cells are non-editable")
    void tableCellsNonEditable() {
        TableModel model = panel.getTableModel();
        assertFalse(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(0, 1));
    }
}

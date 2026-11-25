package vn.clinicmanagement.desktop.modules.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử bảo toàn hình học giao diện EHC HIS cho VienPhiHoaDonManHinh:
 * - Split pane ngang (bệnh nhân bên trái, chi tiết dịch vụ & tổng kết bên phải)
 * - Bảng danh sách hóa đơn 13 cột mô hình (hiển thị Tên Bệnh Nhân & Tuổi)
 * - Bảng chi tiết dịch vụ 11 cột chuẩn EHC HIS
 * - Phím tắt F2 (Thu tiền), F7 (In) và nút Làm Mới
 * - Thanh tổng kết footer với các nhãn Tổng chi phí, Đã thu, Còn nợ.
 */
class VienPhiHoaDonManHinhGiaoDienBaoToanTest {

    private static final List<String> INVOICE_COLUMNS = List.of(
            "STT", "Số Phiếu Thu", "Mã BN", "Tên Bệnh Nhân", "Ngày Lập",
            "Đối Tượng", "Tổng Tiền", "BHYT Chi Trả", "BN Cần Trả", "Đã Thu", "Còn Nợ", "Trạng Thái", "Tuổi"
    );

    private static final List<String> DETAIL_COLUMNS = List.of(
            "Tên Dịch Vụ", "Đơn Vị", "Đối Tượng", "Số Lượng", "Đơn Giá",
            "Tỷ Lệ TT", "Thành Tiền", "BHYT Trả", "Tỷ Lệ BHTT", "BN Trả", "Đã Thu", "YC Thu"
    );

    @Test
    @DisplayName("Invoice table preserves read-only 13-column model schema with 2 visible patient columns")
    void invoiceTablePreservesOriginalTwelveColumnSchema() {
        VienPhiHoaDonManHinh panel = new VienPhiHoaDonManHinh();
        TableModel model = panel.getInvoiceTableModel();

        assertEquals(INVOICE_COLUMNS.size(), model.getColumnCount(), "Must have exactly 13 model columns");
        assertEquals(2, panel.getInvoiceTable().getColumnCount(), "Must show Tên Bệnh Nhân and Tuổi");
        for (int i = 0; i < INVOICE_COLUMNS.size(); i++) {
            assertEquals(INVOICE_COLUMNS.get(i), model.getColumnName(i));
            assertFalse(model.isCellEditable(0, i), "Cell must be read-only at col " + i);
        }
    }

    @Test
    @DisplayName("Detail table preserves read-only 11-column schema")
    void detailTablePreservesOriginalTenColumnSchema() {
        VienPhiHoaDonManHinh panel = new VienPhiHoaDonManHinh();
        TableModel model = panel.getDetailTableModel();

        assertEquals(DETAIL_COLUMNS.size(), model.getColumnCount(), "Must have exactly 11 columns");
        for (int i = 0; i < DETAIL_COLUMNS.size(); i++) {
            assertEquals(DETAIL_COLUMNS.get(i), model.getColumnName(i));
            assertFalse(model.isCellEditable(0, i), "Cell must be read-only at col " + i);
        }
    }

    @Test
    @DisplayName("Center split pane preserves horizontal split orientation and resize weight")
    void centerSplitPanePreservesVerticalOrientationAndDividerLocation360() {
        VienPhiHoaDonManHinh panel = new VienPhiHoaDonManHinh();
        List<JSplitPane> splitPanes = descendantsOfType(panel, JSplitPane.class);

        assertEquals(1, splitPanes.size());
        JSplitPane splitPane = splitPanes.get(0);

        assertEquals(JSplitPane.HORIZONTAL_SPLIT, splitPane.getOrientation());
        assertEquals(2, splitPane.getDividerSize());
        assertEquals(0.22, splitPane.getResizeWeight(), 0.001);
    }

    @Test
    @DisplayName("Toolbar preserves action buttons F2, F7, Refresh and keyboard bindings")
    void toolbarPreservesActionButtonsAndKeyBindings() {
        VienPhiHoaDonManHinh panel = new VienPhiHoaDonManHinh();
        assertNotNull(panel.getBtnCollect());
        assertNotNull(panel.getBtnPrintSlip());
        assertNotNull(panel.getBtnRefresh());
        assertEquals("▣  Lưu Phiếu (F2)", panel.getBtnCollect().getText());
        assertEquals("In", panel.getBtnPrintSlip().getText());
        assertEquals("Làm Mới", panel.getBtnRefresh().getText());

        assertNotNull(panel.getToolbar());
        List<JButton> toolbarButtons = descendantsOfType(panel.getToolbar(), JButton.class).stream()
                .filter(b -> b.getText() != null && !b.getText().isBlank())
                .toList();
        assertEquals(5, toolbarButtons.size());
        assertEquals("In", toolbarButtons.get(1).getText());
        assertEquals("Làm Mới", toolbarButtons.get(2).getText());
        assertEquals("Tìm Kiếm", toolbarButtons.get(3).getText());

        // Kiểm tra phím tắt F2 và F7 đã được đăng ký trên InputMap
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke("F2")), "F2 keybinding must be registered");
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke("F7")), "F7 keybinding must be registered");

        ActionMap actionMap = panel.getActionMap();
        assertNotNull(actionMap.get("actionCollectPayment"), "actionCollectPayment action must exist");
        assertNotNull(actionMap.get("actionOpenBillingSlip"), "actionOpenBillingSlip action must exist");
    }

    @Test
    @DisplayName("Summary footer preserves labels and metric counters")
    void summaryFooterPreservesLabelsAndStyling() {
        VienPhiHoaDonManHinh panel = new VienPhiHoaDonManHinh();

        assertNotNull(panel.getLblTotalAmount());
        assertNotNull(panel.getLblPaidAmount());
        assertNotNull(panel.getLblUnpaidAmount());
        assertEquals("0đ", panel.getLblTotalAmount().getText());
        assertEquals("0đ", panel.getLblPaidAmount().getText());
        assertEquals("0đ", panel.getLblUnpaidAmount().getText());

        List<JLabel> labels = descendantsOfType(panel, JLabel.class);
        assertTrue(labels.stream().anyMatch(l -> "Tổng chi phí:".equals(l.getText())),
                "Summary footer must display Tổng chi phí:");
        assertTrue(labels.stream().anyMatch(l -> "Đã thu:".equals(l.getText())),
                "Summary footer must display Đã thu:");
        assertTrue(labels.stream().anyMatch(l -> "Còn nợ:".equals(l.getText())),
                "Summary footer must display Còn nợ:");
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container) {
                matches.addAll(descendantsOfType((Container) component, type));
            }
        }
        return matches;
    }
}

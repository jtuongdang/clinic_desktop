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
 * Kiểm thử bảo toàn 100% hình học giao diện EHC HIS cho BillingInvoicePanel:
 * - Split pane dọc dividerLocation 360, dividerSize 5
 * - Bảng danh sách hóa đơn 12 cột chuẩn EHC HIS
 * - Bảng chi tiết hóa đơn 10 cột chuẩn EHC HIS
 * - Phím tắt F2 (Thu Tiền), F7 (In Bảng Kê) và nút Làm Mới
 * - Thanh tổng kết footer và các nhãn tổng doanh thu, đã thu, còn nợ.
 */
class BillingInvoicePanelUiPreservationTest {

    private static final List<String> INVOICE_COLUMNS = List.of(
            "STT", "Số Phiếu Thu", "Mã BN", "Họ Và Tên Bệnh Nhân", "Ngày Lập",
            "Đối Tượng", "Tổng Tiền", "BHYT Chi Trả", "BN Cần Trả", "Đã Thu", "Còn Nợ", "Trạng Thái"
    );

    private static final List<String> DETAIL_COLUMNS = List.of(
            "STT", "Khoa/Phòng Thực Hiện", "Tên Dịch Vụ / Thuốc / Vật Tư", "ĐVT",
            "Số Lượng", "Đơn Giá", "Thành Tiền", "Tỷ Lệ BHYT", "BHYT Trả", "Bệnh Nhân Trả"
    );

    @Test
    @DisplayName("Invoice table preserves original read-only 12-column schema")
    void invoiceTablePreservesOriginalTwelveColumnSchema() {
        BillingInvoicePanel panel = new BillingInvoicePanel();
        TableModel model = panel.getInvoiceTableModel();

        assertEquals(INVOICE_COLUMNS.size(), model.getColumnCount(), "Must have exactly 12 columns");
        for (int i = 0; i < INVOICE_COLUMNS.size(); i++) {
            assertEquals(INVOICE_COLUMNS.get(i), model.getColumnName(i));
            assertFalse(model.isCellEditable(0, i), "Cell must be read-only at col " + i);
        }
    }

    @Test
    @DisplayName("Detail table preserves original read-only 10-column schema")
    void detailTablePreservesOriginalTenColumnSchema() {
        BillingInvoicePanel panel = new BillingInvoicePanel();
        TableModel model = panel.getDetailTableModel();

        assertEquals(DETAIL_COLUMNS.size(), model.getColumnCount(), "Must have exactly 10 columns");
        for (int i = 0; i < DETAIL_COLUMNS.size(); i++) {
            assertEquals(DETAIL_COLUMNS.get(i), model.getColumnName(i));
            assertFalse(model.isCellEditable(0, i), "Cell must be read-only at col " + i);
        }
    }

    @Test
    @DisplayName("Center split pane preserves vertical split orientation and divider location 360")
    void centerSplitPanePreservesVerticalOrientationAndDividerLocation360() {
        BillingInvoicePanel panel = new BillingInvoicePanel();
        List<JSplitPane> splitPanes = descendantsOfType(panel, JSplitPane.class);

        assertEquals(1, splitPanes.size());
        JSplitPane splitPane = splitPanes.get(0);

        assertEquals(JSplitPane.VERTICAL_SPLIT, splitPane.getOrientation());
        assertEquals(360, splitPane.getDividerLocation());
        assertEquals(5, splitPane.getDividerSize());
    }

    @Test
    @DisplayName("Toolbar preserves action buttons F2, F7, Refresh and keyboard bindings")
    void toolbarPreservesActionButtonsAndKeyBindings() {
        BillingInvoicePanel panel = new BillingInvoicePanel();
        assertNotNull(panel.getBtnCollect());
        assertNotNull(panel.getBtnPrintSlip());
        assertNotNull(panel.getBtnRefresh());
        assertEquals(" Thu Tiền (F2)", panel.getBtnCollect().getText());
        assertEquals(" In Bảng Kê (F7)", panel.getBtnPrintSlip().getText());
        assertEquals("🔄 Làm Mới", panel.getBtnRefresh().getText());

        // Kiểm tra phím tắt F2 và F7 đã được đăng ký trên InputMap
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke("F2")), "F2 keybinding must be registered");
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke("F7")), "F7 keybinding must be registered");

        ActionMap actionMap = panel.getActionMap();
        assertNotNull(actionMap.get("actionCollectPayment"), "actionCollectPayment action must exist");
        assertNotNull(actionMap.get("actionOpenBillingSlip"), "actionOpenBillingSlip action must exist");
    }

    @Test
    @DisplayName("Summary footer preserves labels, styling, and guide tip")
    void summaryFooterPreservesLabelsAndStyling() {
        BillingInvoicePanel panel = new BillingInvoicePanel();

        assertNotNull(panel.getLblTotalAmount());
        assertNotNull(panel.getLblPaidAmount());
        assertNotNull(panel.getLblUnpaidAmount());

        assertTrue(panel.getLblTotalAmount().getText().startsWith("Tổng doanh thu:"));
        assertTrue(panel.getLblPaidAmount().getText().startsWith("Đã thanh toán:"));
        assertTrue(panel.getLblUnpaidAmount().getText().startsWith("Còn công nợ:"));

        List<JLabel> labels = descendantsOfType(panel, JLabel.class);
        boolean foundGuide = false;
        for (JLabel lbl : labels) {
            String text = lbl.getText();
            if (text != null && text.contains("Mẫu 01/KCCB chuẩn Bộ Y Tế")) {
                foundGuide = true;
                break;
            }
        }
        assertTrue(foundGuide, "Guide label for billing slip must be present");
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

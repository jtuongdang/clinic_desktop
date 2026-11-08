package vn.clinicmanagement.desktop.modules.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogPanel Wiring & Clean Architecture Integration Tests (HD-INTEG-04)")
class CatalogPanelWiringTest {

    private CatalogPanel panel;

    @BeforeEach
    void setUp() {
        CatalogCompositionRoot.reset();
        panel = new CatalogPanel();
    }

    @AfterEach
    void tearDown() {
        CatalogCompositionRoot.reset();
    }

    @Test
    @DisplayName("Khởi tạo CatalogPanel thành công và hiển thị danh mục mặc định")
    void testInitializationInHeadlessMode() {
        assertNotNull(panel, "CatalogPanel không được null");
        assertEquals("catalog", panel.getViewKey(), "ViewKey của CatalogPanel phải là 'catalog' đồng bộ với Dashboard");
        assertNotNull(panel.getTable(), "Table bảng danh mục phải khởi tạo");
        assertNotNull(panel.getTableModel(), "TableModel không được null");
        assertEquals(0, panel.getCurrentCategoryIndex(), "Mặc định mở danh mục ICD-10");
        assertEquals("ICD-10 (Bệnh tật BYT)", panel.getCurrentCategory());

        DefaultTableModel model = panel.getTableModel();
        assertTrue(model.getRowCount() > 0, "Bảng ICD-10 phải có dữ liệu mặc định hiển thị");
        assertEquals("Mã ICD-10", model.getColumnName(0));
    }

    @Test
    @DisplayName("Chuyển đổi qua lại giữa 4 danh mục và xác nhận cột tương ứng")
    void testSwitchingAcrossAllFourCategories() {
        for (int i = 0; i < CatalogPanel.CATEGORY_NAMES.length; i++) {
            panel.selectCategory(i);
            assertEquals(i, panel.getCurrentCategoryIndex());
            assertEquals(CatalogPanel.CATEGORY_NAMES[i], panel.getCurrentCategory());

            DefaultTableModel model = panel.getTableModel();
            String[] expectedHeaders = CatalogPanel.COLUMN_HEADERS[i];
            assertEquals(expectedHeaders.length, model.getColumnCount(), "Số cột danh mục " + i + " phải khớp");

            for (int col = 0; col < expectedHeaders.length; col++) {
                assertEquals(expectedHeaders[col], model.getColumnName(col), "Tiêu đề cột " + col + " phải khớp");
            }
            assertTrue(model.getRowCount() > 0, "Danh mục " + CatalogPanel.CATEGORY_NAMES[i] + " phải nạp được dữ liệu");
        }
    }

    @Test
    @DisplayName("Tìm kiếm lọc dữ liệu thời gian thực (Realtime Search)")
    void testRealtimeSearchFiltering() {
        // 1. Tìm kiếm trên ICD-10
        panel.selectCategory(0);
        panel.search("huyết áp");
        DefaultTableModel model = panel.getTableModel();
        assertTrue(model.getRowCount() >= 1, "Tìm 'huyết áp' phải có ít nhất 1 kết quả");
        boolean matched = false;
        for (int r = 0; r < model.getRowCount(); r++) {
            String name = String.valueOf(model.getValueAt(r, 1));
            if (name.toLowerCase().contains("huyết áp")) {
                matched = true;
                break;
            }
        }
        assertTrue(matched, "Tên bệnh phải chứa từ khóa tìm kiếm");

        // Tìm từ khóa không tồn tại
        panel.search("NON_EXISTENT_KEYWORD_XYZ");
        assertEquals(0, panel.getTableModel().getRowCount(), "Từ khóa không tồn tại phải trả về 0 dòng");

        // 2. Tìm kiếm trên Dược / Thuốc
        panel.selectCategory(2);
        panel.search("Amox");
        model = panel.getTableModel();
        assertTrue(model.getRowCount() >= 1, "Tìm 'Amox' phải tìm thấy Amoxicillin");
        String drugName = String.valueOf(model.getValueAt(0, 1));
        assertTrue(drugName.toLowerCase().contains("amox"));
    }

    @Test
    @DisplayName("Thực hiện hành động thêm mới an toàn qua Clean Architecture Handlers")
    void testProgrammaticAddNewRecordsAcrossCategories() {
        // Thêm ICD-10 mới
        panel.selectCategory(0);
        boolean addedIcd = panel.addNewRecord("B01", "Bệnh thủy đậu do Varicella", "Thủy đậu");
        assertTrue(addedIcd);
        panel.search("B01");
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("B01", panel.getTableModel().getValueAt(0, 0));

        // Thêm Dịch vụ kỹ thuật mới
        panel.selectCategory(1);
        boolean addedService = panel.addNewRecord("DV-TEST-99", "Đo mật độ xương DEXA", "DICH_VU_KY_THUAT", "350000", "200000");
        assertTrue(addedService);
        panel.search("DV-TEST-99");
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("DV-TEST-99", panel.getTableModel().getValueAt(0, 0));

        // Thêm Thuốc mới (gọi CreateItemHandler)
        panel.selectCategory(2);
        boolean addedDrug = panel.addNewRecord("MED-VITC-500", "Vitamin C 500mg", "Acid ascorbic", "500mg", "Viên", "1200");
        assertTrue(addedDrug);
        panel.search("MED-VITC-500");
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("MED-VITC-500", panel.getTableModel().getValueAt(0, 0));

        // Thêm Phòng khám mới (gọi CreateRoomHandler)
        panel.selectCategory(3);
        boolean addedRoom = panel.addNewRecord("PK-VIP-88", "Phòng Khám VIP 88", "1", "Tầng 8");
        assertTrue(addedRoom);
        panel.search("PK-VIP-88");
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("PK-VIP-88", panel.getTableModel().getValueAt(0, 0));
    }

    @Test
    @DisplayName("Cập nhật trạng thái Khóa / Mở khóa an toàn")
    void testUpdateStatusSafeInHeadless() {
        panel.selectCategory(0);
        panel.search("I10");
        assertEquals(1, panel.getTableModel().getRowCount());

        // Chọn dòng I10
        panel.getTable().setRowSelectionInterval(0, 0);
        boolean updated = panel.updateSelectedStatus(false);
        assertTrue(updated);

        // Xác nhận trạng thái chuyển thành 'Đã khóa'
        panel.search("I10");
        int lastCol = panel.getTableModel().getColumnCount() - 1;
        assertEquals("Đã khóa", panel.getTableModel().getValueAt(0, lastCol));

        // Mở khóa lại
        panel.getTable().setRowSelectionInterval(0, 0);
        boolean unlocked = panel.updateSelectedStatus(true);
        assertTrue(unlocked);
        panel.search("I10");
        assertEquals("Hoạt động", panel.getTableModel().getValueAt(0, lastCol));
    }

    @Test
    @DisplayName("Các nút bấm UI được bọc an toàn trong môi trường Headless không văng Exception")
    void testGuiButtonsDoNotThrowInHeadless() {
        assertDoesNotThrow(() -> {
            panel.getBtnAdd().doClick();
            panel.getBtnEdit().doClick();
            panel.getBtnLock().doClick();
            panel.getBtnRefresh().doClick();
        }, "Bấm các nút trong môi trường Headless không được ném HeadlessException");
    }
}

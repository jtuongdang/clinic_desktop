package vn.clinicmanagement.desktop.modules.pharmacy.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyCategoryTreePanel.CategoryFilter;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PharmacyCategoryTreeWiringTest - Kiểm thử Cây Danh Mục Kho Dược (Task 2.2 - HD-INTEG-02)")
class PharmacyCategoryTreeWiringTest {

    private PharmacyCategoryTreePanel treePanel;

    @BeforeEach
    void setUp() {
        treePanel = new PharmacyCategoryTreePanel();
    }

    @Test
    @DisplayName("Khởi tạo panel không lỗi, thiết lập kích thước và nhãn mặc định chuẩn EHC")
    void testPanelInitialization() {
        assertNotNull(treePanel, "PharmacyCategoryTreePanel không được null");
        assertEquals(215, treePanel.getPreferredSize().width, "Chiều rộng cố định phải là 215px");
        assertEquals(0, treePanel.getSelectedIndex(), "Chỉ số hàng chọn mặc định phải là 0 (Tất cả phiếu)");
        assertEquals(PharmacyCategoryTreePanel.DEFAULT_WAREHOUSE_TITLE, treePanel.getWarehouseTitle());
        assertNull(treePanel.getWarehouseId(), "Mã kho ban đầu là null khi chưa gán");

        // Kiểm tra sự tồn tại của các thành phần giao diện
        assertNotNull(treePanel.getLblTitle());
        assertNotNull(treePanel.getLblAll());
        assertNotNull(treePanel.getLblImport());
        assertNotNull(treePanel.getLblExport());
        assertNotNull(treePanel.getLblPrescription());

        assertNotNull(treePanel.getRowAllTickets());
        assertNotNull(treePanel.getRowImport());
        assertNotNull(treePanel.getRowExport());
        assertNotNull(treePanel.getRowPrescription());

        // Kiểm tra màu sắc mặc định của hàng đầu tiên (đang chọn)
        assertEquals(Color.WHITE, treePanel.getLblAll().getForeground());
        assertEquals(Color.BLACK, treePanel.getLblImport().getForeground());
    }

    @Test
    @DisplayName("Thiết lập tiêu đề kho và mã kho hoạt động chính xác")
    void testSetWarehouseTitleAndId() {
        treePanel.setWarehouseTitle("Kho Tổng Dược Ngoại Trú");
        assertEquals("Kho Tổng Dược Ngoại Trú", treePanel.getWarehouseTitle());
        assertEquals("Kho Tổng Dược Ngoại Trú", treePanel.getLblTitle().getText());

        treePanel.setWarehouseId(10);
        assertEquals(10, treePanel.getWarehouseId());

        // Thiết lập đồng thời cả id và tên kho
        treePanel.setWarehouse(25, "Tủ Trực Cấp Cứu A1");
        assertEquals(25, treePanel.getWarehouseId());
        assertEquals("Tủ Trực Cấp Cứu A1", treePanel.getWarehouseTitle());
        assertEquals("Tủ Trực Cấp Cứu A1", treePanel.getLblTitle().getText());

        // Khi truyền null hoặc rỗng, quay về tiêu đề mặc định
        treePanel.setWarehouseTitle("");
        assertEquals(PharmacyCategoryTreePanel.DEFAULT_WAREHOUSE_TITLE, treePanel.getWarehouseTitle());
        treePanel.setWarehouseTitle(null);
        assertEquals(PharmacyCategoryTreePanel.DEFAULT_WAREHOUSE_TITLE, treePanel.getWarehouseTitle());
    }

    @Test
    @DisplayName("Cập nhật số lượng badge cho các mục danh mục phiếu")
    void testUpdateTicketCounts() {
        treePanel.updateTicketCounts(50, 15, 20, 15);

        assertEquals(50, treePanel.getAllCount());
        assertEquals(15, treePanel.getImportCount());
        assertEquals(20, treePanel.getExportCount());
        assertEquals(15, treePanel.getPrescriptionCount());

        assertEquals("Tất Cả Phiếu (50)", treePanel.getLblAll().getText());
        assertEquals("Phiếu Nhập Kho (15)", treePanel.getLblImport().getText());
        assertEquals("Phiếu Xuất Kho (20)", treePanel.getLblExport().getText());
        assertEquals("Đơn Thuốc Chờ Phát (15)", treePanel.getLblPrescription().getText());
    }

    @Test
    @DisplayName("Chọn dòng bằng selectRow và selectCategory kích hoạt đúng callback CategoryFilter")
    void testSelectRowAndCategoryFilterCallback() {
        AtomicReference<CategoryFilter> capturedFilter = new AtomicReference<>();
        treePanel.setOnCategorySelected(capturedFilter::set);
        treePanel.setWarehouseId(101);

        // Chọn Phiếu Nhập Kho (index 1)
        treePanel.selectRow(1);
        assertEquals(1, treePanel.getSelectedIndex());
        assertNotNull(capturedFilter.get());
        assertEquals(101, capturedFilter.get().warehouseId());
        assertEquals(CategoryFilter.NHAP_KHO, capturedFilter.get().ticketType());
        assertEquals(Color.WHITE, treePanel.getLblImport().getForeground());
        assertEquals(Color.BLACK, treePanel.getLblAll().getForeground());

        // Chọn Phiếu Xuất Kho (index 2)
        treePanel.selectCategory(CategoryFilter.XUAT_KHO);
        assertEquals(2, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.XUAT_KHO, capturedFilter.get().ticketType());
        assertEquals(Color.WHITE, treePanel.getLblExport().getForeground());
        assertEquals(Color.BLACK, treePanel.getLblImport().getForeground());

        // Chọn Đơn Thuốc Chờ Phát (index 3)
        treePanel.selectCategory(CategoryFilter.DON_THUOC);
        assertEquals(3, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.DON_THUOC, capturedFilter.get().ticketType());
        assertEquals(Color.WHITE, treePanel.getLblPrescription().getForeground());
        assertEquals(Color.BLACK, treePanel.getLblExport().getForeground());

        // Quay lại Tất Cả Phiếu (index 0)
        treePanel.selectCategory(CategoryFilter.ALL);
        assertEquals(0, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.ALL, capturedFilter.get().ticketType());
        assertEquals(Color.WHITE, treePanel.getLblAll().getForeground());
        assertEquals(Color.BLACK, treePanel.getLblPrescription().getForeground());
    }

    @Test
    @DisplayName("Giả lập sự kiện chuột click trên các hàng panel kích hoạt chuyển chọn và listener")
    void testSimulatedMouseClicksTriggerSelection() {
        AtomicReference<CategoryFilter> capturedFilter = new AtomicReference<>();
        treePanel.setOnCategorySelected(capturedFilter::set);
        treePanel.setWarehouseId(200);

        // Click vào Hàng Đơn Thuốc Chờ Phát
        simulateMouseClick(treePanel.getRowPrescription());
        assertEquals(3, treePanel.getSelectedIndex());
        assertNotNull(capturedFilter.get());
        assertEquals(CategoryFilter.DON_THUOC, capturedFilter.get().ticketType());
        assertEquals(200, capturedFilter.get().warehouseId());

        // Click vào Hàng Phiếu Xuất Kho
        simulateMouseClick(treePanel.getRowExport());
        assertEquals(2, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.XUAT_KHO, capturedFilter.get().ticketType());

        // Click vào Hàng Phiếu Nhập Kho
        simulateMouseClick(treePanel.getRowImport());
        assertEquals(1, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.NHAP_KHO, capturedFilter.get().ticketType());

        // Click vào Hàng Tất Cả Phiếu
        simulateMouseClick(treePanel.getRowAllTickets());
        assertEquals(0, treePanel.getSelectedIndex());
        assertEquals(CategoryFilter.ALL, capturedFilter.get().ticketType());
    }

    private void simulateMouseClick(Component component) {
        for (MouseListener listener : component.getMouseListeners()) {
            listener.mousePressed(new MouseEvent(
                    component,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    10,
                    10,
                    1,
                    false
            ));
        }
    }
}
